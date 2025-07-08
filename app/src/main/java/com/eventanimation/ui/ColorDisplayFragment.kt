package com.eventanimation.ui

import android.graphics.Color
import android.os.Bundle
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

class ColorDisplayFragment : Fragment() {
    
    private var _binding: FragmentColorDisplayBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    
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
    
    private fun setupObservers() {
        viewModel.displayColor.observe(viewLifecycleOwner) { color ->
            if (color.isNotEmpty()) {
                try {
                    val colorInt = Color.parseColor(color)
                    binding.colorBackground.setBackgroundColor(colorInt)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show()
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
        
        // Optional: Add gesture to toggle back button visibility
        binding.colorBackground.setOnClickListener {
            binding.backButton.visibility = if (binding.backButton.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupFullscreen()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Restore system UI
        requireActivity().window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        _binding = null
    }
}