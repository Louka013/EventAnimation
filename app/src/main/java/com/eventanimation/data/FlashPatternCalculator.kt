package com.eventanimation.data

import com.eventanimation.data.models.SeatInfo

class FlashPatternCalculator {
    
    companion object {
        const val DEFAULT_FREQUENCY = 2 // Hz
        const val DEFAULT_DUTY_CYCLE = 0.5 // 50% on, 50% off
    }
    
    data class FlashPattern(
        val isEvenSeat: Boolean,
        val frequency: Int,
        val dutyCycle: Double,
        val shouldFlashInEvenPhase: Boolean
    )
    
    fun calculateFlashPattern(seatInfo: SeatInfo, frequency: Int = DEFAULT_FREQUENCY): FlashPattern {
        val isEvenSeat = seatInfo.seatNumber % 2 == 0
        
        return FlashPattern(
            isEvenSeat = isEvenSeat,
            frequency = frequency,
            dutyCycle = DEFAULT_DUTY_CYCLE,
            shouldFlashInEvenPhase = isEvenSeat
        )
    }
    
    fun calculateFlashPattern(seatNumber: Int, frequency: Int = DEFAULT_FREQUENCY): FlashPattern {
        val isEvenSeat = seatNumber % 2 == 0
        
        return FlashPattern(
            isEvenSeat = isEvenSeat,
            frequency = frequency,
            dutyCycle = DEFAULT_DUTY_CYCLE,
            shouldFlashInEvenPhase = isEvenSeat
        )
    }
    
    fun isValidFrequency(frequency: Int): Boolean {
        return frequency in 1..10 // Limit to 1-10 Hz for safety
    }
    
    fun isValidDutyCycle(dutyCycle: Double): Boolean {
        return dutyCycle in 0.1..0.9 // 10% to 90% duty cycle
    }
    
    fun shouldFlashAtPhase(pattern: FlashPattern, currentPhase: TimingManager.FlashPhase): Boolean {
        return when (currentPhase) {
            TimingManager.FlashPhase.EVEN_ON_ODD_OFF -> pattern.isEvenSeat
            TimingManager.FlashPhase.EVEN_OFF_ODD_ON -> !pattern.isEvenSeat
            TimingManager.FlashPhase.STOPPED -> false
        }
    }
    
    fun getFlashDescription(pattern: FlashPattern): String {
        val seatType = if (pattern.isEvenSeat) "Even" else "Odd"
        val phaseDescription = if (pattern.shouldFlashInEvenPhase) "first phase" else "second phase"
        return "$seatType seat: Flash during $phaseDescription at ${pattern.frequency}Hz"
    }
}