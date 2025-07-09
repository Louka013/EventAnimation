package com.eventanimation.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimingManager {
    private val handler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timingJob: Job? = null
    
    private val _currentPhase = MutableLiveData<FlashPhase>()
    val currentPhase: LiveData<FlashPhase> = _currentPhase
    
    private val _syncTimestamp = MutableLiveData<Long>()
    val syncTimestamp: LiveData<Long> = _syncTimestamp
    
    data class FlashTiming(
        val frequency: Int = 2, // Hz
        val dutyCycle: Double = 0.5, // 50% on, 50% off
        val syncTimestamp: Long = 0
    )
    
    enum class FlashPhase {
        EVEN_ON_ODD_OFF,
        EVEN_OFF_ODD_ON,
        STOPPED
    }
    
    fun startFlashTiming(
        timing: FlashTiming,
        isEvenSeat: Boolean,
        onFlashChange: (shouldFlash: Boolean) -> Unit
    ) {
        try {
            stopFlashTiming()
            
            val periodMs = 1000 / timing.frequency
            val onDurationMs = (periodMs * timing.dutyCycle).toLong()
            val offDurationMs = periodMs - onDurationMs
            
            isRunning = true
            
            timingJob = scope.launch {
            // Calculate initial phase based on sync timestamp
            val currentTime = SystemClock.elapsedRealtime()
            val elapsedSinceSync = currentTime - timing.syncTimestamp
            val cyclePosition = elapsedSinceSync % (periodMs * 2) // Full cycle = 2 periods
            
            var currentPhaseLocal = if (cyclePosition < periodMs) {
                FlashPhase.EVEN_ON_ODD_OFF
            } else {
                FlashPhase.EVEN_OFF_ODD_ON
            }
            
            while (isRunning) {
                _currentPhase.value = currentPhaseLocal
                
                val shouldFlash = when (currentPhaseLocal) {
                    FlashPhase.EVEN_ON_ODD_OFF -> isEvenSeat
                    FlashPhase.EVEN_OFF_ODD_ON -> !isEvenSeat
                    FlashPhase.STOPPED -> false
                }
                
                onFlashChange(shouldFlash)
                
                // Wait for the appropriate duration
                val waitTime = if (shouldFlash) onDurationMs else offDurationMs
                delay(waitTime)
                
                // Switch phase
                currentPhaseLocal = when (currentPhaseLocal) {
                    FlashPhase.EVEN_ON_ODD_OFF -> FlashPhase.EVEN_OFF_ODD_ON
                    FlashPhase.EVEN_OFF_ODD_ON -> FlashPhase.EVEN_ON_ODD_OFF
                    FlashPhase.STOPPED -> FlashPhase.STOPPED
                }
            }
        }
        
        Log.d("TimingManager", "Flash timing started for ${if (isEvenSeat) "even" else "odd"} seat")
        } catch (e: Exception) {
            Log.e("TimingManager", "Error starting flash timing: ${e.message}")
            isRunning = false
        }
    }
    
    fun stopFlashTiming() {
        isRunning = false
        timingJob?.cancel()
        timingJob = null
        flashRunnable?.let { handler.removeCallbacks(it) }
        flashRunnable = null
        _currentPhase.value = FlashPhase.STOPPED
        Log.d("TimingManager", "Flash timing stopped")
    }
    
    fun synchronizeWithTimestamp(timestamp: Long) {
        _syncTimestamp.value = timestamp
        Log.d("TimingManager", "Synchronized with timestamp: $timestamp")
    }
    
    fun getCurrentSyncTimestamp(): Long {
        return _syncTimestamp.value ?: SystemClock.elapsedRealtime()
    }
    
    fun isCurrentlyRunning(): Boolean = isRunning
    
    fun calculatePhaseOffset(isEvenSeat: Boolean, timestamp: Long): Long {
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedSinceSync = currentTime - timestamp
        
        // For even seats: phase 0 = flash on, phase 1 = flash off
        // For odd seats: phase 0 = flash off, phase 1 = flash on
        return if (isEvenSeat) {
            elapsedSinceSync % 2000 // 2-second cycle at 1Hz
        } else {
            (elapsedSinceSync + 1000) % 2000 // 1-second offset for odd seats
        }
    }
}