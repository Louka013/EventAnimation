package com.eventanimation.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseRepository {
    
    private val database = FirebaseDatabase.getInstance()
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    init {
        setupRemoteConfig()
    }
    
    private fun setupRemoteConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values
        val defaults = mapOf(
            "color_theme_enabled" to true,
            "default_blue_color" to "#0000FF",
            "default_red_color" to "#FF0000",
            "flash_enabled" to true,
            "default_flash_frequency" to 2,
            "max_flash_frequency" to 10,
            "flash_duty_cycle" to 0.5,
            "flash_safety_enabled" to true
        )
        remoteConfig.setDefaultsAsync(defaults)
    }
    
    suspend fun initializeAnonymousAuth(): String {
        return try {
            if (auth.currentUser == null) {
                val result = auth.signInAnonymously().await()
                result.user?.uid ?: ""
            } else {
                auth.currentUser?.uid ?: ""
            }
        } catch (e: Exception) {
            // For demo purposes, return a fake user ID when Firebase is not properly configured
            println("Firebase auth failed: ${e.message}. Using demo mode.")
            "demo_user_${System.currentTimeMillis()}"
        }
    }
    
    suspend fun fetchRemoteConfig(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getSeatColor(seatKey: String): String? {
        return try {
            val snapshot = database.reference
                .child("seatColors")
                .child(seatKey)
                .get()
                .await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun setSeatColor(seatKey: String, color: String) {
        try {
            database.reference
                .child("seatColors")
                .child(seatKey)
                .setValue(color)
                .await()
        } catch (e: Exception) {
            // Silently fail - not critical for app functionality
        }
    }
    
    fun getRemoteConfigValue(key: String): String {
        return remoteConfig.getString(key)
    }
    
    fun getRemoteConfigBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
    
    suspend fun getUserDeviceId(): String {
        return initializeAnonymousAuth()
    }
    
    suspend fun getFlashPattern(seatKey: String): Map<String, Any>? {
        return try {
            val snapshot = database.reference
                .child("flashPatterns")
                .child(seatKey)
                .get()
                .await()
            snapshot.getValue(HashMap::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun setFlashPattern(seatKey: String, pattern: Map<String, Any>) {
        try {
            database.reference
                .child("flashPatterns")
                .child(seatKey)
                .setValue(pattern)
                .await()
        } catch (e: Exception) {
            // Silently fail - not critical for app functionality
        }
    }
    
    suspend fun getFlashSyncData(): Map<String, Any>? {
        return try {
            val snapshot = database.reference
                .child("flashSync")
                .get()
                .await()
            snapshot.getValue(HashMap::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun setFlashSyncData(syncData: Map<String, Any>) {
        try {
            database.reference
                .child("flashSync")
                .setValue(syncData)
                .await()
        } catch (e: Exception) {
            // Silently fail - not critical for app functionality
        }
    }
    
    suspend fun joinFlashEvent(deviceId: String, eventId: String) {
        try {
            val participantData = mapOf(
                "deviceId" to deviceId,
                "eventId" to eventId,
                "timestamp" to System.currentTimeMillis()
            )
            database.reference
                .child("participants")
                .child(deviceId)
                .setValue(participantData)
                .await()
        } catch (e: Exception) {
            // Silently fail - not critical for app functionality
        }
    }
    
    suspend fun leaveFlashEvent(deviceId: String) {
        try {
            database.reference
                .child("participants")
                .child(deviceId)
                .removeValue()
                .await()
        } catch (e: Exception) {
            // Silently fail - not critical for app functionality
        }
    }
    
    fun getRemoteConfigInt(key: String): Int {
        return remoteConfig.getLong(key).toInt()
    }
    
    fun getRemoteConfigDouble(key: String): Double {
        return remoteConfig.getDouble(key)
    }
}