package com.eventanimation.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler private constructor() {
    companion object {
        private const val PREF_NAME = "crash_handler"
        private const val CRASH_COUNT_KEY = "crash_count"
        private const val LAST_CRASH_TIME_KEY = "last_crash_time"
        private const val MAX_CRASHES_PER_HOUR = 3
        
        @Volatile
        private var INSTANCE: CrashHandler? = null
        
        fun getInstance(): CrashHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashHandler().also { INSTANCE = it }
            }
        }
    }
    
    fun handleException(context: Context, exception: Exception, location: String): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            val lastCrashTime = prefs.getLong(LAST_CRASH_TIME_KEY, 0)
            val crashCount = prefs.getInt(CRASH_COUNT_KEY, 0)
            
            // Reset count if it's been more than an hour
            val adjustedCrashCount = if (currentTime - lastCrashTime > 3600000) {
                0
            } else {
                crashCount
            }
            
            // Log the crash
            val stackTrace = StringWriter()
            exception.printStackTrace(PrintWriter(stackTrace))
            Log.e("CrashHandler", "Crash in $location: ${exception.message}")
            Log.e("CrashHandler", "Stack trace: $stackTrace")
            
            // Update crash count
            prefs.edit()
                .putInt(CRASH_COUNT_KEY, adjustedCrashCount + 1)
                .putLong(LAST_CRASH_TIME_KEY, currentTime)
                .apply()
            
            // Return true if we should continue trying, false if we should give up
            return adjustedCrashCount < MAX_CRASHES_PER_HOUR
            
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error in crash handler: ${e.message}")
            return false
        }
    }
    
    fun shouldAttemptFlashInitialization(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            val lastCrashTime = prefs.getLong(LAST_CRASH_TIME_KEY, 0)
            val crashCount = prefs.getInt(CRASH_COUNT_KEY, 0)
            
            // Reset count if it's been more than an hour
            return if (currentTime - lastCrashTime > 3600000) {
                true
            } else {
                crashCount < MAX_CRASHES_PER_HOUR
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error checking crash status: ${e.message}")
            return true // Default to allowing attempts
        }
    }
}