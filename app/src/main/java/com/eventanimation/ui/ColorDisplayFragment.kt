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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.eventanimation.databinding.FragmentColorDisplayBinding
import com.eventanimation.ui.viewmodels.MainViewModel
import com.eventanimation.utils.AnimationConstants
import java.text.SimpleDateFormat
import java.util.*

class ColorDisplayFragment : Fragment() {
    
    private var _binding: FragmentColorDisplayBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    
    // Animation configuration
    private val ANIMATION_FREQUENCY = AnimationConstants.ANIMATION_FREQUENCY
    private val START_TIME = AnimationConstants.START_TIME
    
    // Animation variables
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    private var targetColor: String = ""
    private var isAnimationActive = false
    
    // Flash variables
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlash = false
    private var isFlashModeEnabled = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeCameraFlash()
        } else {
            Toast.makeText(context, "Camera permission required for flash mode", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        setupFullscreen()
        setupObservers()
        setupClickListeners()
        initializeAnimation()
        checkFlashModeAndSetup()
    }
    
    private fun setupFullscreen() {
        // Hide system UI for fullscreen experience
        requireActivity().window.apply {
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    private fun initializeAnimation() {
        animationHandler = Handler(Looper.getMainLooper())
    }
    
    private fun checkFlashModeAndSetup() {
        // Check if flash mode is enabled
        isFlashModeEnabled = viewModel.isFlashModeEnabled.value ?: false
        
        if (isFlashModeEnabled) {
            // Flash mode ON - set black background and initialize camera flash
            binding.colorBackground.setBackgroundColor(Color.BLACK)
            checkPermissionsAndInitialize()
        } else {
            // Flash mode OFF - normal color mode (will be set when color is available)
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
                cameraId = cameraIdList[0]
                Log.d("ColorDisplayFragment", "Camera flash initialized successfully")
            } else {
                Log.w("ColorDisplayFragment", "Camera flash not available")
                hasFlash = false
            }
        } catch (e: Exception) {
            Log.e("ColorDisplayFragment", "Error initializing camera flash: ${e.message}")
            hasFlash = false
        }
    }
    
    private fun setupObservers() {
        viewModel.displayColor.observe(viewLifecycleOwner) { color ->
            if (color.isNotEmpty()) {
                targetColor = color
                // Start animation immediately when color is set
                startAnimation()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Optional: Add gesture to toggle back button visibility
        binding.colorBackground.setOnClickListener {
            binding.backButton.visibility = if (binding.backButton.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }
    
    private fun isTimeToStartAnimation(): Boolean {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val startTime = START_TIME
        
        return try {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTimeDate = timeFormat.parse(currentTime)
            val startTimeDate = timeFormat.parse(startTime)
            
            currentTimeDate?.time ?: 0L >= startTimeDate?.time ?: 0L
        } catch (e: Exception) {
            false
        }
    }
    
    private fun is_blink_on(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        val cycleTimeMs = (1000.0 / ANIMATION_FREQUENCY).toLong()
        val halfCycleMs = cycleTimeMs / 2
        
        return (currentTimeMillis % cycleTimeMs) < halfCycleMs
    }
    
    private fun startAnimation() {
        if (isAnimationActive) {
            stopAnimation()
        }
        
        // Always start the animation logic, but behavior depends on time
        isAnimationActive = true
        val updateIntervalMs = (1000.0 / (ANIMATION_FREQUENCY * 10)).toLong() // Update 10 times per cycle for smooth animation
        
        animationRunnable = object : Runnable {
            override fun run() {
                if (isAnimationActive && _binding != null) {
                    if (isTimeToStartAnimation()) {
                        updateBlinkingColor()
                    } else {
                        // Before start time, display solid color
                        displaySolidColor()
                    }
                    animationHandler?.postDelayed(this, updateIntervalMs)
                }
            }
        }
        
        animationRunnable?.let { runnable ->
            animationHandler?.post(runnable)
        }
    }
    
    private fun updateBlinkingColor() {
        try {
            if (isFlashModeEnabled && hasFlash) {
                // Flash mode: use camera flash
                val shouldFlash = is_blink_on()
                if (shouldFlash) {
                    turnOnCameraFlash()
                } else {
                    turnOffCameraFlash()
                }
                // Keep background black in flash mode
                binding.colorBackground.setBackgroundColor(Color.BLACK)
            } else {
                // Color mode: use screen colors
                val shouldShowTargetColor = is_blink_on()
                val colorToDisplay = if (shouldShowTargetColor) {
                    targetColor
                } else {
                    "#000000" // Black
                }
                
                val colorInt = Color.parseColor(colorToDisplay)
                binding.colorBackground.setBackgroundColor(colorInt)
            }
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show()
            displaySolidColor() // Fallback to solid color
        }
        
        // Hide countdown timer during animation
        hideCountdownTimer()
    }
    
    private fun displaySolidColor() {
        // Before start time, display only white background with timer
        if (!isTimeToStartAnimation()) {
            binding.colorBackground.setBackgroundColor(Color.WHITE)
            showCountdownTimer()
        } else {
            // After start time, display the target color
            try {
                if (targetColor.isNotEmpty()) {
                    val colorInt = Color.parseColor(targetColor)
                    binding.colorBackground.setBackgroundColor(colorInt)
                }
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show()
            }
            hideCountdownTimer()
        }
    }
    
    private fun stopAnimation() {
        isAnimationActive = false
        animationRunnable?.let { runnable ->
            animationHandler?.removeCallbacks(runnable)
        }
        animationRunnable = null
        
        // Ensure flash is off when stopping
        if (isFlashModeEnabled) {
            turnOffCameraFlash()
        }
    }
    
    private fun turnOnCameraFlash() {
        try {
            cameraManager?.setTorchMode(cameraId ?: "", true)
        } catch (e: CameraAccessException) {
            Log.e("ColorDisplayFragment", "Error turning on camera flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("ColorDisplayFragment", "Unexpected error with camera flash: ${e.message}")
        }
    }
    
    private fun turnOffCameraFlash() {
        try {
            cameraManager?.setTorchMode(cameraId ?: "", false)
        } catch (e: CameraAccessException) {
            Log.e("ColorDisplayFragment", "Error turning off camera flash: ${e.message}")
        } catch (e: Exception) {
            Log.e("ColorDisplayFragment", "Unexpected error with camera flash: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupFullscreen()
        if (targetColor.isNotEmpty()) {
            startAnimation()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopAnimation()
    }
    
    private fun showCountdownTimer() {
        val timeRemaining = calculateTimeRemaining()
        if (timeRemaining > 0) {
            binding.countdownTimer.text = formatTimeRemaining(timeRemaining)
            binding.countdownTimer.visibility = View.VISIBLE
            
            // Set white background with padding for better visibility
            binding.countdownTimer.setBackgroundColor(Color.WHITE)
            binding.countdownTimer.setPadding(32, 16, 32, 16)
        } else {
            hideCountdownTimer()
        }
    }
    
    private fun hideCountdownTimer() {
        binding.countdownTimer.visibility = View.GONE
    }
    
    private fun calculateTimeRemaining(): Long {
        return try {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTimeDate = timeFormat.parse(currentTime)
            val startTimeDate = timeFormat.parse(START_TIME)
            
            val currentMillis = currentTimeDate?.time ?: 0L
            val startMillis = startTimeDate?.time ?: 0L
            
            if (startMillis > currentMillis) {
                startMillis - currentMillis
            } else {
                0L
            }
        } catch (e: Exception) {
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Stop animation and clean up
        stopAnimation()
        animationHandler = null
        
        // Ensure flash is off
        if (isFlashModeEnabled) {
            turnOffCameraFlash()
        }
        
        // Restore system UI
        try {
            requireActivity().window.apply {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            Log.e("ColorDisplayFragment", "Error restoring system UI: ${e.message}")
        }
        
        _binding = null
    }
}
