package com.eventanimation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.eventanimation.data.FlashPatternCalculator
import com.eventanimation.data.models.SeatInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlashDisplayFragmentTest {
    
    @Test
    fun testFlashPatternCalculator() {
        val calculator = FlashPatternCalculator()
        val seatInfo = SeatInfo("A", 1, 12)
        
        val pattern = calculator.calculateFlashPattern(seatInfo, 2)
        
        assert(pattern.isEvenSeat) // seat 12 is even
        assert(pattern.frequency == 2)
        assert(pattern.shouldFlashInEvenPhase) // even seat should flash in even phase
    }
    
    @Test
    fun testFlashPatternForOddSeat() {
        val calculator = FlashPatternCalculator()
        val seatInfo = SeatInfo("B", 2, 13)
        
        val pattern = calculator.calculateFlashPattern(seatInfo, 3)
        
        assert(!pattern.isEvenSeat) // seat 13 is odd
        assert(pattern.frequency == 3)
        assert(!pattern.shouldFlashInEvenPhase) // odd seat should not flash in even phase
    }
    
    @Test
    fun testValidFrequency() {
        val calculator = FlashPatternCalculator()
        
        assert(calculator.isValidFrequency(2))
        assert(calculator.isValidFrequency(5))
        assert(!calculator.isValidFrequency(0))
        assert(!calculator.isValidFrequency(15))
    }
}