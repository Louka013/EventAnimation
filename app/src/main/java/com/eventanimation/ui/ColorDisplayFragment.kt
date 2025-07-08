package com.eventanimation.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.eventanimation.databinding.FragmentColorDisplayBinding
import com.eventanimation.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class ColorDisplayFragment : Fragment() {
    
    private var _binding: FragmentColorDisplayBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    
    // Animation configuration
    private val ANIMATION_FREQUENCY = 2.0 // Hz - Change this to your desired frequency
    private val START_TIME = "20:53:10" // 24-hour format with seconds - Change this to your desired start time
    
    // Animation variables
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    private var targetColor: String = ""
    private var isAnimationActive = false
    
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
            val shouldShowTargetColor = is_blink_on()
            val colorToDisplay = if (shouldShowTargetColor) {
                targetColor
            } else {
                "#000000" // Black
            }
            
            val colorInt = Color.parseColor(colorToDisplay)
            binding.colorBackground.setBackgroundColor(colorInt)
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
        
        // Restore system UI
        requireActivity().window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        _binding = null
    }
}
