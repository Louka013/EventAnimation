package com.eventanimation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.eventanimation.R
import com.eventanimation.databinding.FragmentInputBinding
import com.eventanimation.ui.viewmodels.MainViewModel

class InputFragment : Fragment() {
    
    private var _binding: FragmentInputBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.submitButton.isEnabled = !isLoading
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        viewModel.displayColor.observe(viewLifecycleOwner) { color ->
            if (color.isNotEmpty()) {
                findNavController().navigate(R.id.action_input_to_color_display)
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.submitButton.setOnClickListener {
            val eventDetails = binding.eventDetailsInput.text.toString().trim()
            val seatInfo = binding.seatInfoInput.text.toString().trim()
            
            if (validateInput(eventDetails, seatInfo)) {
                viewModel.processSeating(eventDetails, seatInfo)
            }
        }
    }
    
    private fun validateInput(eventDetails: String, seatInfo: String): Boolean {
        return when {
            eventDetails.isEmpty() -> {
                binding.eventDetailsLayout.error = getString(R.string.error_empty_fields)
                false
            }
            seatInfo.isEmpty() -> {
                binding.seatInfoLayout.error = getString(R.string.error_empty_fields)
                false
            }
            else -> {
                binding.eventDetailsLayout.error = null
                binding.seatInfoLayout.error = null
                true
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}