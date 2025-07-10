package com.eventanimation.utils

object AnimationConstants {
    // Change this time to set when animations/flash should start
    const val START_TIME = "18:59:30" // 24-hour format with seconds
    const val ANIMATION_FREQUENCY = 2.0 // Hz
    const val FLASH_FREQUENCY = 5.0 // Hz
    
    // Animation type constants
    enum class AnimationType {
        COLOR_ANIMATION,    // Red/Blue color animation
        FLASH_ANIMATION     // Camera flash animation
    }
    
    // Choose which animation type to use
    val CURRENT_ANIMATION_TYPE = AnimationType.FLASH_ANIMATION
}
