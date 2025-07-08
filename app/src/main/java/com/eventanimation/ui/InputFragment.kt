package com.eventanimation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
    
    // Event data structure similar to menuEvent
    data class Event(val name: String, val sections: List<String>)
    
    // Sample event data
    private val events = listOf(
        Event("Football Stadium", listOf("North Stand", "South Stand", "East Stand", "West Stand")),
        Event("Concert Hall", listOf("Balcony", "Orchestra", "Mezzanine")),
        Event("Theater", listOf("Stalls", "Circle", "Gallery")),
        Event("Basketball Arena", listOf("Lower Bowl", "Upper Bowl", "Suite Level"))
    )
    
    private var selectedEvent: Event? = null
    private var selectedSection: String? = null
    private var selectedRow: Int? = null
    private var selectedSeat: Int? = null
    
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
        
        setupDropdowns()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupDropdowns() {
        // Setup Event dropdown
        val eventAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            events.map { it.name }
        )
        binding.eventDropdown.setAdapter(eventAdapter)
        
        binding.eventDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedEvent = events[position]
            selectedSection = null
            selectedRow = null
            selectedSeat = null
            
            // Clear other dropdowns
            binding.sectionDropdown.text.clear()
            binding.rowDropdown.text.clear()
            binding.seatDropdown.text.clear()
            
            // Setup Section dropdown based on selected event
            setupSectionDropdown()
            updateSubmitButton()
        }
        
        // Setup Row dropdown (1-30)
        val rowAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            (1..30).map { it.toString() }
        )
        binding.rowDropdown.setAdapter(rowAdapter)
        
        binding.rowDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedRow = position + 1
            updateSubmitButton()
        }
        
        // Setup Seat dropdown (1-40)
        val seatAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            (1..40).map { it.toString() }
        )
        binding.seatDropdown.setAdapter(seatAdapter)
        
        binding.seatDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedSeat = position + 1
            updateSubmitButton()
        }
    }
    
    private fun setupSectionDropdown() {
        if (_binding == null) return // Safety check
        
        selectedEvent?.let { event ->
            val sectionAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                event.sections
            )
            binding.sectionDropdown.setAdapter(sectionAdapter)
            
            binding.sectionDropdown.setOnItemClickListener { _, _, position, _ ->
                selectedSection = event.sections[position]
                updateSubmitButton()
            }
        }
    }
    
    private fun updateSubmitButton() {
        if (_binding == null) return // Safety check to prevent crashes
        
        val isValid = selectedEvent != null && 
                     selectedSection != null && 
                     selectedRow != null && 
                     selectedSeat != null
        val isLoading = viewModel.isLoading.value ?: false
        binding.submitButton.isEnabled = isValid && !isLoading
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            updateSubmitButton()
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
            if (validateInput()) {
                val eventDetails = selectedEvent?.name ?: ""
                val seatInfo = "$selectedSection, Row $selectedRow, Seat $selectedSeat"
                viewModel.processSeating(eventDetails, seatInfo)
            }
        }
    }
    
    private fun validateInput(): Boolean {
        return when {
            selectedEvent == null -> {
                Toast.makeText(context, "Please select an event", Toast.LENGTH_SHORT).show()
                false
            }
            selectedSection == null -> {
                Toast.makeText(context, "Please select a section", Toast.LENGTH_SHORT).show()
                false
            }
            selectedRow == null -> {
                Toast.makeText(context, "Please select a row", Toast.LENGTH_SHORT).show()
                false
            }
            selectedSeat == null -> {
                Toast.makeText(context, "Please select a seat number", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}