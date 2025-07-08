package com.eventanimation.data

import com.eventanimation.data.models.SeatInfo

class SeatParser {
    
    fun parseSeatInfo(seatInfoString: String): SeatInfo? {
        return try {
            // Support multiple formats:
            // "Section A, Row 5, Seat 12"
            // "A-5-12"
            // "Section: A, Row: 5, Seat: 12"
            
            when {
                seatInfoString.contains("Section") && seatInfoString.contains("Row") && seatInfoString.contains("Seat") -> {
                    parseVerboseFormat(seatInfoString)
                }
                seatInfoString.contains("-") -> {
                    parseCompactFormat(seatInfoString)
                }
                else -> {
                    parseFlexibleFormat(seatInfoString)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseVerboseFormat(input: String): SeatInfo? {
        val regex = Regex("Section\\s*:?\\s*(\\w+).*Row\\s*:?\\s*(\\d+).*Seat\\s*:?\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(input) ?: return null
        
        return SeatInfo(
            section = match.groupValues[1].uppercase(),
            row = match.groupValues[2].toInt(),
            seatNumber = match.groupValues[3].toInt()
        )
    }
    
    private fun parseCompactFormat(input: String): SeatInfo? {
        val parts = input.split("-")
        if (parts.size != 3) return null
        
        return SeatInfo(
            section = parts[0].trim().uppercase(),
            row = parts[1].trim().toInt(),
            seatNumber = parts[2].trim().toInt()
        )
    }
    
    private fun parseFlexibleFormat(input: String): SeatInfo? {
        // Try to extract numbers and letters
        val letterRegex = Regex("[A-Za-z]+")
        val numberRegex = Regex("\\d+")
        
        val letters = letterRegex.findAll(input).map { it.value }.toList()
        val numbers = numberRegex.findAll(input).map { it.value.toInt() }.toList()
        
        if (letters.isNotEmpty() && numbers.size >= 2) {
            return SeatInfo(
                section = letters[0].uppercase(),
                row = numbers[0],
                seatNumber = numbers[1]
            )
        }
        
        return null
    }
}