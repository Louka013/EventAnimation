package com.eventanimation.data.models

data class EventDetails(
    val name: String,
    val venue: String,
    val additionalInfo: String? = null
)