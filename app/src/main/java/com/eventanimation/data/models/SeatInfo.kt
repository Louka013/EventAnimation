package com.eventanimation.data.models

data class SeatInfo(
    val section: String,
    val row: Int,
    val seatNumber: Int
) {
    fun toKey(): String = "${section}_${row}_${seatNumber}"
}