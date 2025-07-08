# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EventAnimation is an Android mobile application that displays full-screen colors based on user seat information at events. The app uses Firebase for authentication, remote configuration, and real-time database synchronization.

## Architecture

- **Platform**: Android (Kotlin)
- **Architecture Pattern**: MVVM with LiveData
- **Backend**: Firebase (Authentication, Remote Config, Realtime Database)
- **UI**: Single Activity with Navigation Component

## Key Features

1. **Seat Information Parsing**: Flexible parsing supporting multiple formats (e.g., "Section A, Row 5, Seat 12", "A-5-12")
2. **Color Calculation**: Even seat numbers = Blue (#0000FF), Odd seat numbers = Red (#FF0000)
3. **Firebase Integration**: 
   - Anonymous authentication for device tracking
   - Remote Config for theme customization
   - Realtime Database for seat-specific color overrides
4. **Full-Screen Display**: Immersive color display with system UI hidden

## Development Commands

```bash
# Build the project
./gradlew assembleDebug

# Run tests
./gradlew test

# Install debug APK
./gradlew installDebug

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

## Project Structure

```
app/src/main/java/com/eventanimation/
├── data/
│   ├── models/          # Data models (SeatInfo, EventDetails)
│   ├── FirebaseRepository.kt
│   ├── SeatColorCalculator.kt
│   └── SeatParser.kt
├── ui/
│   ├── viewmodels/      # ViewModels
│   ├── MainActivity.kt
│   ├── InputFragment.kt
│   └── ColorDisplayFragment.kt
```

## Firebase Configuration

- **google-services.json**: Contains demo configuration (replace with actual Firebase project)
- **Remote Config**: Supports dynamic color theme updates
- **Realtime Database**: Structure: `seatColors/{section}_{row}_{seat}: "#color"`
- **Anonymous Auth**: Automatically signs in users for device tracking

## Key Components

- **SeatParser**: Handles multiple seat format inputs with flexible regex matching
- **SeatColorCalculator**: Implements even/odd seat color logic with validation
- **FirebaseRepository**: Manages all Firebase operations with coroutines
- **MainViewModel**: Coordinates data flow and handles business logic
- **Navigation**: Uses Navigation Component for fragment transitions

## Color Logic Priority

1. Firebase Database override (highest priority)
2. Remote Config colors (medium priority)
3. Default calculated colors (lowest priority)

## Error Handling

- Input validation with user-friendly messages
- Network error handling for Firebase operations
- Graceful degradation when Firebase is unavailable
- Color format validation

## Testing

Run unit tests with: `./gradlew test`
Run instrumentation tests with: `./gradlew connectedAndroidTest`

## Firebase Setup

To set up Firebase for this project:
1. Create a Firebase project at console.firebase.google.com
2. Add Android app with package name: `com.eventanimation`
3. Download and replace `google-services.json`
4. Enable Authentication (Anonymous)
5. Enable Realtime Database
6. Enable Remote Config with keys: `default_blue_color`, `default_red_color`, `color_theme_enabled`