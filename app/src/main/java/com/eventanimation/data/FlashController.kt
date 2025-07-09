package com.eventanimation.data

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlashController(private val context: Context) {
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val handler = Handler(Looper.getMainLooper())
    private var cameraId: String? = null
    private var isFlashOn = false
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _flashState = MutableLiveData<Boolean>()
    val flashState: LiveData<Boolean> = _flashState
    
    private val _flashAvailable = MutableLiveData<Boolean>()
    val flashAvailable: LiveData<Boolean> = _flashAvailable
    
    init {
        try {
            initializeCamera()
        } catch (e: Exception) {
            Log.e("FlashController", "Failed to initialize camera: ${e.message}")
            _flashAvailable.value = false
        }
    }
    
    private fun initializeCamera() {
        scope.launch {
            try {
                val cameraIds = cameraManager.cameraIdList
                for (id in cameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val flashAvailable = characteristics.get(
                        android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) ?: false
                    
                    if (flashAvailable) {
                        cameraId = id
                        _flashAvailable.value = true
                        Log.d("FlashController", "Flash available on camera $id")
                        return@launch
                    }
                }
                _flashAvailable.value = false
                Log.w("FlashController", "No flash available on device")
            } catch (e: CameraAccessException) {
                Log.e("FlashController", "Camera access error: ${e.message}")
                _flashAvailable.value = false
            } catch (e: Exception) {
                Log.e("FlashController", "Unexpected error during camera initialization: ${e.message}")
                _flashAvailable.value = false
            }
        }
    }
    
    fun turnOnFlash() {
        if (cameraId == null || isFlashOn) return
        
        try {
            cameraManager.setTorchMode(cameraId!!, true)
            isFlashOn = true
            _flashState.value = true
            Log.d("FlashController", "Flash turned ON")
        } catch (e: CameraAccessException) {
            Log.e("FlashController", "Error turning on flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("FlashController", "Unexpected error turning on flash: ${e.message}")
        }
    }
    
    fun turnOffFlash() {
        if (cameraId == null || !isFlashOn) return
        
        try {
            cameraManager.setTorchMode(cameraId!!, false)
            isFlashOn = false
            _flashState.value = false
            Log.d("FlashController", "Flash turned OFF")
        } catch (e: CameraAccessException) {
            Log.e("FlashController", "Error turning off flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("FlashController", "Unexpected error turning off flash: ${e.message}")
        }
    }
    
    fun toggleFlash() {
        if (isFlashOn) {
            turnOffFlash()
        } else {
            turnOnFlash()
        }
    }
    
    fun isFlashCurrentlyOn(): Boolean = isFlashOn
    
    fun cleanup() {
        turnOffFlash()
    }
}