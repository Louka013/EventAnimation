package com.eventanimation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.eventanimation.ui.viewmodels.MainViewModel
import com.eventanimation.utils.AnimationConstants
import java.text.SimpleDateFormat
import java.util.*

class SimpleFlashDisplayFragment : Fragment() {
    
    private var rootView: LinearLayout? = null
    private lateinit var viewModel: MainViewModel
    
    // Flash configuration
    private val FLASH_FREQUENCY = AnimationConstants.FLASH_FREQUENCY
    private val START_TIME = AnimationConstants.START_TIME
    
    // Flash variables
    private var flashHandler: Handler? = null
    private var flashRunnable: Runnable? = null
    private var isFlashActive = false
    private var statusText: TextView? = null
    private var countdownText: TextView? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlash = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeCameraFlash()
        } else {
            Toast.makeText(context, "Camera permission required for flash", Toast.LENGTH_LONG).show()
            statusText?.text = "Camera permission denied"
            statusText?.setTextColor(Color.RED)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SimpleFlashDisplay", "onCreateView called")
        
        return try {
            // Create a simple LinearLayout programmatically with black background
            rootView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                setBackgroundColor(Color.BLACK)
            }
            
            // Add title
            val titleText = TextView(requireContext()).apply {
                text = "Flash Display Mode"
                textSize = 24f
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 32)
            }
            rootView?.addView(titleText)
            
            // Add status text
            statusText = TextView(requireContext()).apply {
                text = "Initializing flash system..."
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 16)
            }
            rootView?.addView(statusText)
            
            // Add countdown text
            countdownText = TextView(requireContext()).apply {
                text = ""
                textSize = 18f
                setTextColor(Color.YELLOW)
                setPadding(0, 16, 0, 16)
                visibility = View.GONE
            }
            rootView?.addView(countdownText)
            
            // Add back button
            val backButton = Button(requireContext()).apply {
                text = "Back"
                setOnClickListener {
                    try {
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Log.e("SimpleFlashDisplay", "Navigation error: ${e.message}")
                        requireActivity().onBackPressed()
                    }
                }
            }
            rootView?.addView(backButton)
            
            Log.d("SimpleFlashDisplay", "View created successfully")
            rootView
            
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error creating view: ${e.message}", e)
            
            // Create minimal fallback view
            TextView(requireContext()).apply {
                text = "Flash Display Error\nClick to go back"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.BLACK)
                setPadding(32, 32, 32, 32)
                setOnClickListener {
                    try {
                        findNavController().popBackStack()
                    } catch (navException: Exception) {
                        requireActivity().onBackPressed()
                    }
                }
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            Log.d("SimpleFlashDisplay", "onViewCreated called")
            viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            
            setupFullscreen()
            checkPermissionsAndInitialize()
            
            // Show seat information if available
            viewModel.seatInfo.observe(viewLifecycleOwner) { seatInfo ->
                seatInfo?.let { seat ->
                    try {
                        val seatText = TextView(requireContext()).apply {
                            text = "Seat: ${seat.section}, Row ${seat.row}, Seat ${seat.seatNumber}"
                            textSize = 14f
                            setTextColor(Color.WHITE)
                            setPadding(0, 16, 0, 16)
                        }
                        rootView?.addView(seatText, 1) // Add after title
                        
                        val seatType = if (seat.seatNumber % 2 == 0) "Even" else "Odd"
                        val typeText = TextView(requireContext()).apply {
                            text = "Seat Type: $seatType (${if (seat.seatNumber % 2 == 0) "Blue" else "Red"})"
                            textSize = 14f
                            setTextColor(Color.WHITE)
                            setPadding(0, 0, 0, 16)
                        }
                        rootView?.addView(typeText, 2) // Add after seat info
                        
                        // Start flash timer (will show countdown until start time)
                        startFlashTimer()
                        
                    } catch (e: Exception) {
                        Log.e("SimpleFlashDisplay", "Error updating seat info: ${e.message}")
                    }
                }
            }
            
            // Update status
            statusText?.text = "Flash system ready - Black screen + flash mode"
            
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error in onViewCreated: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupFullscreen() {
        try {
            requireActivity().window.apply {
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error setting up fullscreen: ${e.message}")
        }
    }
    
    private fun checkPermissionsAndInitialize() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeCameraFlash()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun initializeCameraFlash() {
        try {
            cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager?.cameraIdList
            
            hasFlash = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            
            if (hasFlash && cameraIdList != null && cameraIdList.isNotEmpty()) {
                // Usually the rear camera is at index 0
                cameraId = cameraIdList[0]
                statusText?.text = "Camera flash ready - Black screen + flash mode"
                statusText?.setTextColor(Color.GREEN)
                Log.d("SimpleFlashDisplay", "Camera flash initialized successfully")
            } else {
                statusText?.text = "Camera flash not available"
                statusText?.setTextColor(Color.RED)
                Toast.makeText(context, "Camera flash not available on this device", Toast.LENGTH_LONG).show()
                Log.w("SimpleFlashDisplay", "Camera flash not available")
            }
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error initializing camera flash: ${e.message}")
            statusText?.text = "Flash initialization error"
            statusText?.setTextColor(Color.RED)
            hasFlash = false
        }
    }
    
    private fun isTimeToStartFlash(): Boolean {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        return try {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTimeDate = timeFormat.parse(currentTime)
            val startTimeDate = timeFormat.parse(START_TIME)
            
            val currentMillis = currentTimeDate?.time ?: 0L
            val startMillis = startTimeDate?.time ?: 0L
            val shouldStart = currentMillis >= startMillis
            
            Log.d("SimpleFlashDisplay", "Current time: $currentTime, Start time: $START_TIME, Should start: $shouldStart")
            
            shouldStart
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error parsing time: ${e.message}")
            false
        }
    }
    
    private fun startFlashTimer() {
        if (!hasFlash) {
            statusText?.text = "Camera flash not available"
            return
        }
        
        if (isFlashActive) {
            stopFlashSequence()
        }
        
        isFlashActive = true
        flashHandler = Handler(Looper.getMainLooper())
        
        // Start timer check (will show countdown until start time)
        flashRunnable = object : Runnable {
            override fun run() {
                if (isFlashActive && rootView != null) {
                    if (isTimeToStartFlash()) {
                        // Time reached - start actual flash sequence
                        countdownText?.visibility = View.GONE
                        startActualFlashSequence()
                    } else {
                        // Show countdown until start time
                        showCountdownTimer()
                        flashHandler?.postDelayed(this, 1000) // Update every second
                    }
                }
            }
        }
        
        flashRunnable?.let { runnable ->
            flashHandler?.post(runnable)
        }
    }
    
    private fun startActualFlashSequence() {
        if (!hasFlash) return
        
        val flashIntervalMs = (1000.0 / FLASH_FREQUENCY).toLong()
        val flashDurationMs = flashIntervalMs / 2 // 50% duty cycle
        
        flashRunnable = object : Runnable {
            override fun run() {
                if (isFlashActive && rootView != null) {
                    // Turn on camera flash (back LED flash)
                    turnOnCameraFlash()
                    
                    // Turn off camera flash after flash duration
                    flashHandler?.postDelayed({
                        if (isFlashActive) {
                            turnOffCameraFlash()
                        }
                    }, flashDurationMs)
                    
                    // Schedule next flash
                    flashHandler?.postDelayed(this, flashIntervalMs)
                }
            }
        }
        
        flashRunnable?.let { runnable ->
            flashHandler?.post(runnable)
        }
    }
    
    private fun turnOnCameraFlash() {
        try {
            cameraManager?.setTorchMode(cameraId ?: "", true)
            Log.d("SimpleFlashDisplay", "Camera flash turned ON")
        } catch (e: CameraAccessException) {
            Log.e("SimpleFlashDisplay", "Error turning on camera flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Unexpected error with camera flash: ${e.message}")
        }
    }
    
    private fun turnOffCameraFlash() {
        try {
            cameraManager?.setTorchMode(cameraId ?: "", false)
            Log.d("SimpleFlashDisplay", "Camera flash turned OFF")
        } catch (e: CameraAccessException) {
            Log.e("SimpleFlashDisplay", "Error turning off camera flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Unexpected error with camera flash: ${e.message}")
        }
    }
    
    private fun stopFlashSequence() {
        isFlashActive = false
        flashRunnable?.let { runnable ->
            flashHandler?.removeCallbacks(runnable)
        }
        flashRunnable = null
        
        // Ensure camera flash is off
        turnOffCameraFlash()
        
        // Keep background black
        rootView?.setBackgroundColor(Color.BLACK)
        statusText?.setTextColor(Color.WHITE)
    }
    
    private fun showCountdownTimer() {
        val timeRemaining = calculateTimeRemaining()
        if (timeRemaining > 0) {
            countdownText?.text = "Flash starts in: ${formatTimeRemaining(timeRemaining)}"
            countdownText?.visibility = View.VISIBLE
        } else {
            countdownText?.visibility = View.GONE
        }
    }
    
    private fun calculateTimeRemaining(): Long {
        return try {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTimeDate = timeFormat.parse(currentTime)
            val startTimeDate = timeFormat.parse(START_TIME)
            
            val currentMillis = currentTimeDate?.time ?: 0L
            val startMillis = startTimeDate?.time ?: 0L
            
            val remaining = if (startMillis > currentMillis) {
                startMillis - currentMillis
            } else {
                0L
            }
            
            Log.d("SimpleFlashDisplay", "Time remaining calculation: Current=$currentTime, Start=$START_TIME, Remaining=${remaining}ms")
            
            remaining
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error calculating time remaining: ${e.message}")
            0L
        }
    }
    
    private fun formatTimeRemaining(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("SimpleFlashDisplay", "onResume called")
        setupFullscreen()
        if (viewModel.seatInfo.value != null) {
            startFlashTimer()
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("SimpleFlashDisplay", "onPause called")
        stopFlashSequence()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("SimpleFlashDisplay", "onDestroyView called")
        
        stopFlashSequence()
        flashHandler = null
        
        // Restore system UI
        try {
            requireActivity().window.apply {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            Log.e("SimpleFlashDisplay", "Error restoring system UI: ${e.message}")
        }
        
        rootView = null
        statusText = null
        countdownText = null
    }
}