package com.eventanimation.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventanimation.data.FirebaseRepository
import com.eventanimation.data.SeatColorCalculator
import com.eventanimation.data.FlashPatternCalculator
import com.eventanimation.data.SeatParser
import com.eventanimation.data.models.EventDetails
import com.eventanimation.data.models.SeatInfo
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val repository = FirebaseRepository()
    private val calculator = SeatColorCalculator()
    private val flashCalculator = FlashPatternCalculator()
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
    
    private val _seatInfo = MutableLiveData<SeatInfo?>()
    val seatInfo: LiveData<SeatInfo?> = _seatInfo
    
    private val _flashPattern = MutableLiveData<FlashPatternCalculator.FlashPattern?>()
    val flashPattern: LiveData<FlashPatternCalculator.FlashPattern?> = _flashPattern
    
    private val _flashFrequency = MutableLiveData<Int>()
    val flashFrequency: LiveData<Int> = _flashFrequency
    
    private val _isFlashModeEnabled = MutableLiveData<Boolean>()
    val isFlashModeEnabled: LiveData<Boolean> = _isFlashModeEnabled
    
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
                _seatInfo.value = seatInfo
                
                // Get flash frequency from remote config
                val frequency = repository.getRemoteConfigInt("default_flash_frequency")
                _flashFrequency.value = frequency
                
                // Calculate flash pattern
                val pattern = flashCalculator.calculateFlashPattern(seatInfo, frequency)
                _flashPattern.value = pattern
                
                // Calculate base color using seat number (even = blue, odd = red)
                val finalColor = if (seatInfo.seatNumber % 2 == 0) {
                    "#0000FF" // Blue for even seats
                } else {
                    "#FF0000" // Red for odd seats
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
    
    fun setFlashModeEnabled(enabled: Boolean) {
        _isFlashModeEnabled.value = enabled
    }
    
    fun updateFlashFrequency(frequency: Int) {
        viewModelScope.launch {
            try {
                if (!flashCalculator.isValidFrequency(frequency)) {
                    _errorMessage.value = "Invalid frequency. Must be between 1-10 Hz"
                    return@launch
                }
                
                _flashFrequency.value = frequency
                
                // Update flash pattern with new frequency
                val seatInfo = _seatInfo.value
                if (seatInfo != null) {
                    val pattern = flashCalculator.calculateFlashPattern(seatInfo, frequency)
                    _flashPattern.value = pattern
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update frequency: ${e.message}"
            }
        }
    }
    
    fun startFlashEvent(eventId: String) {
        viewModelScope.launch {
            try {
                val deviceId = repository.getUserDeviceId()
                repository.joinFlashEvent(deviceId, eventId)
                
                // Set up flash sync data
                val frequency = _flashFrequency.value ?: 2
                val syncData = mapOf(
                    "eventId" to eventId,
                    "frequency" to frequency,
                    "timestamp" to System.currentTimeMillis(),
                    "isActive" to true
                )
                repository.setFlashSyncData(syncData)
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start flash event: ${e.message}"
            }
        }
    }
    
    fun stopFlashEvent() {
        viewModelScope.launch {
            try {
                val deviceId = repository.getUserDeviceId()
                repository.leaveFlashEvent(deviceId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop flash event: ${e.message}"
            }
        }
    }
}