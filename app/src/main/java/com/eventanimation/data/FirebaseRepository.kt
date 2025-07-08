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
            "default_red_color" to "#FF0000"
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
}