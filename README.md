# EventAnimation

**EventAnimation** is an Android mobile application that displays full-screen animated colors based on user seat information at events. The app provides an immersive blinking animation experience with time-based control and dropdown-based seat selection.

## Features

- **Animated Seat-Based Display**: Blinking animation between target color and black for even/odd seat numbers
- **Time-Based Animation Control**: Animation starts at a predefined time (default: 18:30)
- **Dropdown Seat Selection**: User-friendly dropdown menus for event, section, row, and seat selection
- **Configurable Animation**: Adjustable frequency and start time settings
- **Full-Screen Experience**: Immersive color display with hidden system UI
- **Firebase Integration**: Real-time synchronization and remote configuration (demo mode available)
- **Anonymous Authentication**: Automatic device tracking without user accounts

## User Manual

### Getting Started

1. **Installation**: Install the APK on your Android device (minimum Android 5.0)
2. **Launch**: Open the EventAnimation app
3. **Select Seat Information**: Use dropdown menus to select your seating details

### Using the App

#### Step 1: Select Seat Information
When you first open the app, you'll see dropdown menus for seat selection:

**Available Selection Options:**
- **Event**: Football Stadium, Concert Hall, Theater, Basketball Arena
- **Section**: Dynamic options based on selected event (e.g., North Stand, Balcony, Stalls)
- **Row**: Numbers 1-30
- **Seat Number**: Numbers 1-40

#### Step 2: Submit and View Animation
After selecting all required information:
1. Click the "Submit" button
2. The app calculates your seat color
3. Navigate to the full-screen animation display

#### Step 3: Full-Screen Animation Display
The animation screen features:
- **Time-based activation**: Animation starts at configured time (default: 18:30)
- **Blinking animation**: Alternates between target color and black
- **Configurable frequency**: Default 2.0 Hz (2 blinks per second)
- **Hidden system UI** for immersive experience
- **Touch controls**: Tap screen to show/hide back button

### Animation Logic

#### Color Assignment:
- **Even seat numbers**: Blue (#0000FF) ↔ Black (#000000)
- **Odd seat numbers**: Red (#FF0000) ↔ Black (#000000)

#### Time-Based Behavior:
- **Before start time**: Displays solid color
- **After start time**: Displays blinking animation at configured frequency

#### Animation Configuration:
Located in `ColorDisplayFragment.kt`:
```kotlin
private val ANIMATION_FREQUENCY = 2.0 // Hz (blinks per second)
private val START_TIME = "18:30" // 24-hour format
```

### Navigation

- **Back Button**: Return to seat selection screen from animation display
- **Dropdown Navigation**: Use dropdowns to change seat selection
- **System Navigation**: Use Android's back gesture or button to navigate

### Error Handling

The app provides user-friendly error messages for:
- **Missing seat selections**: Prompts to complete all dropdown selections
- **Network issues**: Graceful degradation when Firebase is unavailable
- **Animation errors**: Fallback to solid color display

### Offline Usage

The app works offline with:
- **Local color calculation** (even/odd logic)
- **Dropdown-based seat selection**
- **Local animation rendering**
- **Demo mode** when Firebase is unavailable

### Firebase Features (Optional)

#### Anonymous Authentication
- Automatically signs in users for device tracking
- Demo mode available when Firebase is not configured
- No user accounts required

#### Remote Configuration
- Dynamic color theme updates
- Feature flags for customization
- Real-time configuration changes

#### Realtime Database
- Seat-specific color overrides
- Real-time synchronization across devices
- Custom color assignments for events

## Technical Requirements

- **Android Version**: 5.0 (API level 21) or higher
- **Internet Connection**: Optional (required only for Firebase features)
- **Storage**: Minimal storage requirements (< 10MB)
- **Permissions**: Internet access for Firebase synchronization

## Development Setup

### Prerequisites
- Android Studio 4.0 or higher
- Android SDK 21 or higher
- OpenJDK 21 Development Kit
- Firebase project (optional for basic functionality)

### Building the App

```bash
# Clone the repository
git clone <repository-url>
cd EventAnimation

# Build debug APK
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew assembleDebug

# Install on connected device
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

### Animation Configuration

To customize the animation behavior, modify these values in `ColorDisplayFragment.kt`:

```kotlin
// Animation frequency in Hz (blinks per second)
private val ANIMATION_FREQUENCY = 2.0

// Start time in 24-hour format
private val START_TIME = "18:30"
```

### Firebase Configuration (Optional)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add Android app with package name: `com.eventanimation`
3. Download and replace `google-services.json` in the `app/` directory
4. Enable the following Firebase services:
   - **Authentication**: Enable Anonymous authentication
   - **Realtime Database**: Create database with appropriate rules
   - **Remote Config**: Add configuration keys:
     - `default_blue_color` (default: "#0000FF")
     - `default_red_color` (default: "#FF0000")
     - `color_theme_enabled` (default: true)

### Database Structure

```json
{
  "seatColors": {
    "A_5_12": "#FF00FF",
    "B_3_8": "#00FFFF"
  }
}
```

## Troubleshooting

### App Crashes on Startup
- Ensure OpenJDK 21 Development Kit is installed
- Check that launcher icons are properly generated
- Verify device connectivity

### Animation Not Starting
- Check if current time is after the configured START_TIME
- Verify ANIMATION_FREQUENCY is set to a positive value
- Ensure all seat selections are completed

### Dropdown Menus Not Working
- Verify all dropdown selections are made in order (Event → Section → Row → Seat)
- Check for binding safety in case of rapid navigation
- Ensure fragment lifecycle is properly managed

### Firebase Issues
- App works in demo mode when Firebase is not configured
- Check `google-services.json` file placement and validity
- Verify internet connection for Firebase features

## Support

For technical issues or questions:
1. Check the troubleshooting section above
2. Review Firebase console for configuration issues (if using Firebase)
3. Verify animation configuration values
4. Check device time settings for time-based features

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Version History

- **v1.0.0**: Initial release with basic seat-based color display
- **v1.1.0**: Added Firebase integration and remote configuration
- **v1.2.0**: Enhanced seat parsing and error handling
- **v1.3.0**: Added dropdown-based seat selection interface
- **v1.4.0**: Implemented blinking animation with time-based control

## Key Components

### Animation System
- **ColorDisplayFragment**: Main animation rendering and control
- **Time-based activation**: Configurable start time
- **Frequency control**: Adjustable blinking rate
- **Color alternation**: Target color ↔ Black animation

### User Interface
- **InputFragment**: Dropdown-based seat selection
- **MainActivity**: Navigation and fragment management
- **Material Design**: Modern UI components and styling

### Data Flow
- **Dropdown selection** → **Seat parsing** → **Color calculation** → **Animation rendering**
- **Firebase integration** (optional) → **Real-time updates** → **Color overrides**