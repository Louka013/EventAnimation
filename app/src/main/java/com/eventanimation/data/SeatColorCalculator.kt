package com.eventanimation.data

class SeatColorCalculator {
    
    companion object {
        const val BLUE_COLOR = "#0000FF"
        const val RED_COLOR = "#FF0000"
    }
    
    fun calculateColor(seatNumber: Int): String {
        return if (seatNumber % 2 == 0) {
            BLUE_COLOR
        } else {
            RED_COLOR
        }
    }
    
    fun isValidColor(color: String): Boolean {
        return try {
            android.graphics.Color.parseColor(color)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}