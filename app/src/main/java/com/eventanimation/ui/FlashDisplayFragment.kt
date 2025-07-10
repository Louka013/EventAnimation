package com.eventanimation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.eventanimation.data.FlashController
import com.eventanimation.data.FlashPatternCalculator
import com.eventanimation.data.FlashSyncService
import com.eventanimation.data.TimingManager
import com.eventanimation.databinding.FragmentFlashDisplayBinding
import com.eventanimation.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class FlashDisplayFragment : Fragment() {
    
    private var _binding: FragmentFlashDisplayBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    private var flashController: FlashController? = null
    private var timingManager: TimingManager? = null
    private var flashSyncService: FlashSyncService? = null
    private lateinit var flashPatternCalculator: FlashPatternCalculator
    
    private var flashPattern: FlashPatternCalculator.FlashPattern? = null
    private var isFlashActive = false
    
    // Animation configuration
    private val ANIMATION_FREQUENCY = 2 // Hz - Change this to your desired frequency
    private val START_TIME = "10:25:10" // 24-hour format with seconds - Change this to your desired start time
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeFlashSystem()
        } else {
            showPermissionError()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlashDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            flashPatternCalculator = FlashPatternCalculator()
            
            setupFullscreen()
            setupObservers()
            setupClickListeners()
            checkPermissionsAndInitialize()
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error in onViewCreated: ${e.message}")
            Toast.makeText(context, "Error initializing flash display", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
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
    
    private fun checkPermissionsAndInitialize() {
        try {
            Log.d("FlashDisplayFragment", "Checking camera permissions")
            
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FlashDisplayFragment", "Camera permission granted, initializing flash system")
                    initializeFlashSystem()
                }
                else -> {
                    Log.d("FlashDisplayFragment", "Camera permission not granted, requesting permission")
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error checking permissions: ${e.message}", e)
            Toast.makeText(context, "Error checking camera permissions", Toast.LENGTH_SHORT).show()
            
            // Try to initialize anyway in case permissions are not the issue
            try {
                initializeFlashSystem()
            } catch (initException: Exception) {
                Log.e("FlashDisplayFragment", "Fallback initialization failed: ${initException.message}")
                findNavController().popBackStack()
            }
        }
    }
    
    private fun initializeFlashSystem() {
        try {
            Log.d("FlashDisplayFragment", "Starting flash system initialization")
            
            // Initialize flash controller with error handling
            flashController = FlashController(requireContext())
            Log.d("FlashDisplayFragment", "FlashController initialized")
            
            // Initialize timing manager
            timingManager = TimingManager()
            Log.d("FlashDisplayFragment", "TimingManager initialized")
            
            // Initialize sync service
            flashSyncService = FlashSyncService(requireContext())
            Log.d("FlashDisplayFragment", "FlashSyncService initialized")
            
            // Only set up observers if fragment is still attached
            if (isAdded && _binding != null) {
                setupFlashObservers()
                Log.d("FlashDisplayFragment", "Flash observers set up successfully")
                
                // Show initial state
                updateFlashIndicator(false)
                updatePhaseDisplay(TimingManager.FlashPhase.STOPPED)
                updateSyncStatus(FlashSyncService.SyncState(isConnected = false, isSynced = false))
                updateParticipantCount(0)
            } else {
                Log.w("FlashDisplayFragment", "Fragment not attached, skipping observer setup")
            }
            
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Failed to initialize flash system: ${e.message}", e)
            
            // Show a user-friendly error message
            val errorMsg = when (e) {
                is SecurityException -> "Camera permission required for flash functionality"
                is IllegalStateException -> "Flash system initialization failed"
                else -> "Flash system unavailable: ${e.message}"
            }
            
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            
            // Safely navigate back
            try {
                if (isAdded) {
                    findNavController().popBackStack()
                }
            } catch (navException: Exception) {
                Log.e("FlashDisplayFragment", "Navigation error: ${navException.message}")
                // If navigation fails, at least close the fragment
                requireActivity().onBackPressed()
            }
        }
    }
    
    private fun setupFlashObservers() {
        try {
            flashController?.let { controller ->
                controller.flashAvailable.observe(viewLifecycleOwner) { available ->
                    if (!available) {
                        showFlashUnavailableMessage()
                    }
                }
                
                controller.flashState.observe(viewLifecycleOwner) { isOn ->
                    updateFlashIndicator(isOn)
                }
            }
            
            timingManager?.let { manager ->
                manager.currentPhase.observe(viewLifecycleOwner) { phase ->
                    updatePhaseDisplay(phase)
                }
            }
            
            flashSyncService?.let { service ->
                service.syncState.observe(viewLifecycleOwner) { syncState ->
                    updateSyncStatus(syncState)
                }
                
                service.participantCount.observe(viewLifecycleOwner) { count ->
                    updateParticipantCount(count)
                }
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error setting up flash observers: ${e.message}")
        }
    }
    
    private fun setupObservers() {
        viewModel.seatInfo.observe(viewLifecycleOwner) { seatInfo ->
            seatInfo?.let {
                flashPattern = flashPatternCalculator.calculateFlashPattern(it, ANIMATION_FREQUENCY)
                updateFlashPatternDisplay()
                
                // Check if flash mode is enabled
                val isFlashModeEnabled = viewModel.isFlashModeEnabled
                if (isFlashModeEnabled) {
                    // Flash mode ON: black screen with flash
                    binding.flashBackground.setBackgroundColor(android.graphics.Color.BLACK)
                    startFlashSequence()
                } else {
                    // Flash mode OFF: show color without flash
                    val color = viewModel.displayColor.value ?: "#FFFFFF"
                    binding.flashBackground.setBackgroundColor(android.graphics.Color.parseColor(color))
                    // Don't start flash sequence
                }
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
        
        binding.flashBackground.setOnClickListener {
            binding.backButton.visibility = if (binding.backButton.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        
        binding.emergencyStopButton.setOnClickListener {
            emergencyStop()
        }
    }
    
    private fun startFlashSequence() {
        try {
            if (!isTimeToStartAnimation()) {
                showCountdownTimer()
                return
            }
            
            flashPattern?.let { pattern ->
                val timing = TimingManager.FlashTiming(
                    frequency = pattern.frequency,
                    dutyCycle = pattern.dutyCycle,
                    syncTimestamp = flashSyncService?.getCurrentSyncData()?.globalTimestamp 
                        ?: System.currentTimeMillis()
                )
                
                timingManager?.startFlashTiming(timing, pattern.isEvenSeat) { shouldFlash ->
                    if (shouldFlash) {
                        flashController?.turnOnFlash()
                    } else {
                        flashController?.turnOffFlash()
                    }
                }
                
                isFlashActive = true
                hideCountdownTimer()
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error starting flash sequence: ${e.message}")
        }
    }
    
    private fun stopFlashSequence() {
        try {
            isFlashActive = false
            timingManager?.stopFlashTiming()
            flashController?.turnOffFlash()
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error stopping flash sequence: ${e.message}")
        }
    }
    
    private fun emergencyStop() {
        stopFlashSequence()
        Toast.makeText(context, "Flash sequence stopped", Toast.LENGTH_SHORT).show()
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
    
    private fun updateFlashPatternDisplay() {
        flashPattern?.let { pattern ->
            val description = flashPatternCalculator.getFlashDescription(pattern)
            binding.flashPatternText.text = description
            binding.flashPatternText.visibility = View.VISIBLE
        }
    }
    
    private fun updateFlashIndicator(isOn: Boolean) {
        if (_binding == null) return
        try {
            binding.flashIndicator.text = if (isOn) "FLASH ON" else "FLASH OFF"
            binding.flashIndicator.setTextColor(
                if (isOn) 
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                else 
                    ContextCompat.getColor(requireContext(), android.R.color.white)
            )
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error updating flash indicator: ${e.message}")
        }
    }
    
    private fun updatePhaseDisplay(phase: TimingManager.FlashPhase) {
        if (_binding == null) return
        try {
            binding.phaseIndicator.text = when (phase) {
                TimingManager.FlashPhase.EVEN_ON_ODD_OFF -> "Phase: Even ON, Odd OFF"
                TimingManager.FlashPhase.EVEN_OFF_ODD_ON -> "Phase: Even OFF, Odd ON"
                TimingManager.FlashPhase.STOPPED -> "Phase: STOPPED"
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error updating phase display: ${e.message}")
        }
    }
    
    private fun updateSyncStatus(syncState: FlashSyncService.SyncState) {
        if (_binding == null) return
        try {
            binding.syncStatus.text = when {
                !syncState.isConnected -> "Sync: DISCONNECTED"
                syncState.isSynced -> "Sync: ACTIVE (${syncState.latency}ms)"
                else -> "Sync: INACTIVE"
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error updating sync status: ${e.message}")
        }
    }
    
    private fun updateParticipantCount(count: Int) {
        if (_binding == null) return
        try {
            binding.participantCount.text = "Participants: $count"
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error updating participant count: ${e.message}")
        }
    }
    
    private fun showPermissionError() {
        Toast.makeText(context, "Camera permission required for flash", Toast.LENGTH_LONG).show()
    }
    
    private fun showFlashUnavailableMessage() {
        Toast.makeText(context, "Flash not available on this device", Toast.LENGTH_LONG).show()
        
        // Show fallback UI
        if (_binding != null) {
            try {
                binding.flashIndicator.text = "FLASH UNAVAILABLE"
                binding.flashIndicator.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                )
                binding.flashPatternText.text = "Flash hardware not available on this device"
            } catch (e: Exception) {
                Log.e("FlashDisplayFragment", "Error showing fallback UI: ${e.message}")
            }
        }
    }
    
    private fun showCountdownTimer() {
        val timeRemaining = calculateTimeRemaining()
        if (timeRemaining > 0) {
            binding.countdownTimer.text = formatTimeRemaining(timeRemaining)
            binding.countdownTimer.visibility = View.VISIBLE
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
    
    override fun onResume() {
        super.onResume()
        setupFullscreen()
        if (flashPattern != null && isTimeToStartAnimation()) {
            startFlashSequence()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopFlashSequence()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        try {
            // Stop flash and clean up
            stopFlashSequence()
            flashController?.cleanup()
            flashSyncService?.cleanup()
            
            // Restore system UI
            requireActivity().window.apply {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            Log.e("FlashDisplayFragment", "Error during cleanup: ${e.message}")
        } finally {
            _binding = null
        }
    }
}
