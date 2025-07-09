package com.eventanimation.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.TimeUnit

class FlashSafetyManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("flash_safety", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var safetyCheckRunnable: Runnable? = null
    
    private val _safetyState = MutableLiveData<SafetyState>()
    val safetyState: LiveData<SafetyState> = _safetyState
    
    private val _flashDuration = MutableLiveData<Long>()
    val flashDuration: LiveData<Long> = _flashDuration
    
    private var flashStartTime: Long = 0
    private var totalFlashTime: Long = 0
    private var isFlashActive = false
    
    data class SafetyState(
        val isPhotosensitiveModeEnabled: Boolean,
        val maxFlashDuration: Long,
        val maxFrequency: Int,
        val isSafetyWarningShown: Boolean,
        val canFlash: Boolean,
        val reasonForBlock: String? = null
    )
    
    companion object {
        private const val MAX_FLASH_DURATION_MS = 300000L // 5 minutes
        private const val MAX_CONTINUOUS_DURATION_MS = 30000L // 30 seconds
        private const val MAX_SAFE_FREQUENCY = 3 // 3 Hz max for photosensitive safety
        private const val PHOTOSENSITIVE_MODE_KEY = "photosensitive_mode"
        private const val SAFETY_WARNING_SHOWN_KEY = "safety_warning_shown"
        private const val TOTAL_FLASH_TIME_KEY = "total_flash_time"
        private const val LAST_RESET_TIME_KEY = "last_reset_time"
    }
    
    init {
        loadSafetySettings()
        setupPeriodicSafetyCheck()
    }
    
    private fun loadSafetySettings() {
        val isPhotosensitiveMode = prefs.getBoolean(PHOTOSENSITIVE_MODE_KEY, false)
        val warningShown = prefs.getBoolean(SAFETY_WARNING_SHOWN_KEY, false)
        val savedTotalTime = prefs.getLong(TOTAL_FLASH_TIME_KEY, 0)
        val lastResetTime = prefs.getLong(LAST_RESET_TIME_KEY, System.currentTimeMillis())
        
        // Reset daily flash time if it's been more than 24 hours
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastResetTime > TimeUnit.DAYS.toMillis(1)) {
            totalFlashTime = 0
            prefs.edit()
                .putLong(TOTAL_FLASH_TIME_KEY, 0)
                .putLong(LAST_RESET_TIME_KEY, currentTime)
                .apply()
        } else {
            totalFlashTime = savedTotalTime
        }
        
        updateSafetyState(isPhotosensitiveMode, warningShown)
    }
    
    private fun updateSafetyState(isPhotosensitiveMode: Boolean, warningShown: Boolean) {
        val maxFrequency = if (isPhotosensitiveMode) MAX_SAFE_FREQUENCY else 10
        val maxDuration = if (isPhotosensitiveMode) MAX_CONTINUOUS_DURATION_MS else MAX_FLASH_DURATION_MS
        
        val canFlash = totalFlashTime < maxDuration
        val reasonForBlock = if (!canFlash) {
            "Daily flash limit exceeded. Please wait until tomorrow."
        } else null
        
        _safetyState.value = SafetyState(
            isPhotosensitiveModeEnabled = isPhotosensitiveMode,
            maxFlashDuration = maxDuration,
            maxFrequency = maxFrequency,
            isSafetyWarningShown = warningShown,
            canFlash = canFlash,
            reasonForBlock = reasonForBlock
        )
    }
    
    private fun setupPeriodicSafetyCheck() {
        safetyCheckRunnable = object : Runnable {
            override fun run() {
                if (isFlashActive) {
                    val currentTime = System.currentTimeMillis()
                    val sessionDuration = currentTime - flashStartTime
                    
                    // Check if continuous flash duration exceeds safe limits
                    if (sessionDuration > MAX_CONTINUOUS_DURATION_MS) {
                        Log.w("FlashSafetyManager", "Continuous flash duration exceeded safe limit")
                        // Force stop flash
                        stopFlashSession()
                        return
                    }
                    
                    _flashDuration.value = sessionDuration
                }
                
                // Schedule next check
                handler.postDelayed(this, 1000) // Check every second
            }
        }
        
        handler.post(safetyCheckRunnable!!)
    }
    
    fun startFlashSession(): Boolean {
        val currentState = _safetyState.value
        if (currentState?.canFlash != true) {
            Log.w("FlashSafetyManager", "Flash session blocked: ${currentState?.reasonForBlock}")
            return false
        }
        
        flashStartTime = System.currentTimeMillis()
        isFlashActive = true
        
        Log.d("FlashSafetyManager", "Flash session started")
        return true
    }
    
    fun stopFlashSession() {
        if (isFlashActive) {
            val sessionDuration = System.currentTimeMillis() - flashStartTime
            totalFlashTime += sessionDuration
            
            // Save updated total time
            prefs.edit()
                .putLong(TOTAL_FLASH_TIME_KEY, totalFlashTime)
                .apply()
            
            isFlashActive = false
            _flashDuration.value = 0
            
            // Update safety state
            val currentState = _safetyState.value
            if (currentState != null) {
                updateSafetyState(currentState.isPhotosensitiveModeEnabled, currentState.isSafetyWarningShown)
            }
            
            Log.d("FlashSafetyManager", "Flash session stopped. Session duration: ${sessionDuration}ms, Total today: ${totalFlashTime}ms")
        }
    }
    
    fun enablePhotosensitiveMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PHOTOSENSITIVE_MODE_KEY, enabled)
            .apply()
        
        val currentState = _safetyState.value
        updateSafetyState(enabled, currentState?.isSafetyWarningShown ?: false)
        
        Log.d("FlashSafetyManager", "Photosensitive mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun markSafetyWarningShown() {
        prefs.edit()
            .putBoolean(SAFETY_WARNING_SHOWN_KEY, true)
            .apply()
        
        val currentState = _safetyState.value
        if (currentState != null) {
            updateSafetyState(currentState.isPhotosensitiveModeEnabled, true)
        }
    }
    
    fun isFrequencySafe(frequency: Int): Boolean {
        val currentState = _safetyState.value ?: return false
        return frequency <= currentState.maxFrequency
    }
    
    fun getRemainingFlashTime(): Long {
        val currentState = _safetyState.value ?: return 0
        return maxOf(0, currentState.maxFlashDuration - totalFlashTime)
    }
    
    fun getTotalFlashTimeToday(): Long {
        return totalFlashTime
    }
    
    fun resetDailyLimit() {
        totalFlashTime = 0
        prefs.edit()
            .putLong(TOTAL_FLASH_TIME_KEY, 0)
            .putLong(LAST_RESET_TIME_KEY, System.currentTimeMillis())
            .apply()
        
        val currentState = _safetyState.value
        if (currentState != null) {
            updateSafetyState(currentState.isPhotosensitiveModeEnabled, currentState.isSafetyWarningShown)
        }
    }
    
    fun cleanup() {
        safetyCheckRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        if (isFlashActive) {
            stopFlashSession()
        }
    }
}