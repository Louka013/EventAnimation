package com.eventanimation.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FlashSyncService(private val context: Context) {
    private val database = FirebaseDatabase.getInstance()
    private val syncRef = database.getReference("flashSync")
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _syncState = MutableLiveData<SyncState>()
    val syncState: LiveData<SyncState> = _syncState
    
    private val _globalTimestamp = MutableLiveData<Long>()
    val globalTimestamp: LiveData<Long> = _globalTimestamp
    
    private val _flashFrequency = MutableLiveData<Int>()
    val flashFrequency: LiveData<Int> = _flashFrequency
    
    private val _participantCount = MutableLiveData<Int>()
    val participantCount: LiveData<Int> = _participantCount
    
    private var deviceId: String? = null
    private var syncListener: ValueEventListener? = null
    
    data class SyncState(
        val isConnected: Boolean,
        val isSynced: Boolean,
        val latency: Long = 0
    )
    
    data class FlashSyncData(
        val globalTimestamp: Long,
        val frequency: Int,
        val eventId: String,
        val isActive: Boolean,
        val participantCount: Int = 0
    )
    
    init {
        try {
            deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            setupSyncListener()
        } catch (e: Exception) {
            Log.e("FlashSyncService", "Failed to initialize sync service: ${e.message}")
            _syncState.value = SyncState(isConnected = false, isSynced = false)
        }
    }
    
    private fun setupSyncListener() {
        syncListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val syncData = snapshot.getValue(FlashSyncData::class.java)
                    if (syncData != null) {
                        _globalTimestamp.value = syncData.globalTimestamp
                        _flashFrequency.value = syncData.frequency
                        _participantCount.value = syncData.participantCount
                        
                        // Calculate latency
                        val currentTime = SystemClock.elapsedRealtime()
                        val latency = currentTime - syncData.globalTimestamp
                        
                        _syncState.value = SyncState(
                            isConnected = true,
                            isSynced = syncData.isActive,
                            latency = latency
                        )
                        
                        Log.d("FlashSyncService", "Sync updated: timestamp=${syncData.globalTimestamp}, frequency=${syncData.frequency}, latency=${latency}ms")
                    }
                } catch (e: Exception) {
                    Log.e("FlashSyncService", "Error processing sync data: ${e.message}")
                    _syncState.value = SyncState(isConnected = false, isSynced = false)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("FlashSyncService", "Sync listener cancelled: ${error.message}")
                _syncState.value = SyncState(isConnected = false, isSynced = false)
            }
        }
        
        syncRef.addValueEventListener(syncListener!!)
    }
    
    fun startSync(eventId: String, frequency: Int = 2) {
        scope.launch {
            try {
                val currentTime = SystemClock.elapsedRealtime()
                val syncData = FlashSyncData(
                    globalTimestamp = currentTime,
                    frequency = frequency,
                    eventId = eventId,
                    isActive = true
                )
                
                syncRef.setValue(syncData).await()
                
                // Register this device as a participant
                val participantRef = database.getReference("participants").child(deviceId!!)
                participantRef.setValue(mapOf(
                    "timestamp" to currentTime,
                    "eventId" to eventId,
                    "deviceId" to deviceId
                )).await()
                
                Log.d("FlashSyncService", "Sync started for event: $eventId with frequency: ${frequency}Hz")
                
            } catch (e: Exception) {
                Log.e("FlashSyncService", "Error starting sync: ${e.message}")
            }
        }
    }
    
    fun stopSync() {
        scope.launch {
            try {
                val syncData = FlashSyncData(
                    globalTimestamp = SystemClock.elapsedRealtime(),
                    frequency = 0,
                    eventId = "",
                    isActive = false
                )
                
                syncRef.setValue(syncData).await()
                
                // Remove this device from participants
                deviceId?.let { id ->
                    database.getReference("participants").child(id).removeValue().await()
                }
                
                Log.d("FlashSyncService", "Sync stopped")
                
            } catch (e: Exception) {
                Log.e("FlashSyncService", "Error stopping sync: ${e.message}")
            }
        }
    }
    
    fun updateTimestamp() {
        scope.launch {
            try {
                val currentTime = SystemClock.elapsedRealtime()
                syncRef.child("globalTimestamp").setValue(currentTime).await()
                Log.d("FlashSyncService", "Timestamp updated: $currentTime")
            } catch (e: Exception) {
                Log.e("FlashSyncService", "Error updating timestamp: ${e.message}")
            }
        }
    }
    
    fun joinSync(eventId: String) {
        scope.launch {
            try {
                val currentTime = SystemClock.elapsedRealtime()
                
                // Register this device as a participant
                val participantRef = database.getReference("participants").child(deviceId!!)
                participantRef.setValue(mapOf(
                    "timestamp" to currentTime,
                    "eventId" to eventId,
                    "deviceId" to deviceId
                )).await()
                
                Log.d("FlashSyncService", "Joined sync for event: $eventId")
                
            } catch (e: Exception) {
                Log.e("FlashSyncService", "Error joining sync: ${e.message}")
            }
        }
    }
    
    fun getCurrentSyncData(): FlashSyncData? {
        val timestamp = _globalTimestamp.value ?: return null
        val frequency = _flashFrequency.value ?: return null
        
        return FlashSyncData(
            globalTimestamp = timestamp,
            frequency = frequency,
            eventId = "",
            isActive = _syncState.value?.isSynced ?: false,
            participantCount = _participantCount.value ?: 0
        )
    }
    
    fun cleanup() {
        syncListener?.let { listener ->
            syncRef.removeEventListener(listener)
        }
        scope.launch {
            try {
                deviceId?.let { id ->
                    database.getReference("participants").child(id).removeValue().await()
                }
            } catch (e: Exception) {
                Log.e("FlashSyncService", "Error during cleanup: ${e.message}")
            }
        }
    }
}