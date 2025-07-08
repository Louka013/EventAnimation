package com.eventanimation.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventanimation.data.FirebaseRepository
import com.eventanimation.data.SeatColorCalculator
import com.eventanimation.data.SeatParser
import com.eventanimation.data.models.EventDetails
import com.eventanimation.data.models.SeatInfo
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val repository = FirebaseRepository()
    private val calculator = SeatColorCalculator()
    private val parser = SeatParser()
    
    private val _displayColor = MutableLiveData<String>()
    val displayColor: LiveData<String> = _displayColor
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _currentSeatInfo = MutableLiveData<SeatInfo?>()
    val currentSeatInfo: LiveData<SeatInfo?> = _currentSeatInfo
    
    private val _currentEventDetails = MutableLiveData<EventDetails?>()
    val currentEventDetails: LiveData<EventDetails?> = _currentEventDetails
    
    fun processSeating(eventDetailsString: String, seatInfoString: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Parse event details
                val eventDetails = parseEventDetails(eventDetailsString)
                _currentEventDetails.value = eventDetails
                
                // Parse seat information
                val seatInfo = parser.parseSeatInfo(seatInfoString)
                    ?: throw IllegalArgumentException("Invalid seat format. Please use format like 'Section A, Row 5, Seat 12'")
                
                _currentSeatInfo.value = seatInfo
                
                // Initialize Firebase
                repository.initializeAnonymousAuth()
                repository.fetchRemoteConfig()
                
                // Calculate base color using seat number
                val baseColor = calculator.calculateColor(seatInfo.seatNumber)
                
                // Check for remote config overrides
                val remoteBlue = repository.getRemoteConfigValue("default_blue_color")
                val remoteRed = repository.getRemoteConfigValue("default_red_color")
                
                val calculatedColor = if (seatInfo.seatNumber % 2 == 0) {
                    if (remoteBlue.isNotEmpty() && calculator.isValidColor(remoteBlue)) remoteBlue else baseColor
                } else {
                    if (remoteRed.isNotEmpty() && calculator.isValidColor(remoteRed)) remoteRed else baseColor
                }
                
                // Check Firebase database for seat-specific overrides
                val seatKey = seatInfo.toKey()
                val firebaseColor = repository.getSeatColor(seatKey)
                
                // Use Firebase color if available, otherwise use calculated color
                val finalColor = if (firebaseColor != null && calculator.isValidColor(firebaseColor)) {
                    firebaseColor
                } else {
                    calculatedColor
                }
                
                // Store the calculated color in Firebase if no override exists
                if (firebaseColor == null) {
                    repository.setSeatColor(seatKey, calculatedColor)
                }
                
                _displayColor.value = finalColor
                
            } catch (e: Exception) {
                _errorMessage.value = when (e) {
                    is IllegalArgumentException -> e.message
                    else -> "An error occurred: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun parseEventDetails(eventDetailsString: String): EventDetails {
        // Simple parsing - in a real app, this could be more sophisticated
        val parts = eventDetailsString.split(",", limit = 2)
        return EventDetails(
            name = parts.getOrNull(0)?.trim() ?: eventDetailsString,
            venue = parts.getOrNull(1)?.trim() ?: "",
            additionalInfo = null
        )
    }
    
    fun updateSeatColor(newColor: String) {
        viewModelScope.launch {
            try {
                if (!calculator.isValidColor(newColor)) {
                    _errorMessage.value = "Invalid color format"
                    return@launch
                }
                
                val seatInfo = _currentSeatInfo.value
                if (seatInfo != null) {
                    val seatKey = seatInfo.toKey()
                    repository.setSeatColor(seatKey, newColor)
                    _displayColor.value = newColor
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update color: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}