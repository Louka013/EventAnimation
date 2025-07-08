# EventAnimation

**EventAnimation** is an Android mobile application that displays full-screen colors based on user seat information at events. The app provides an immersive color experience synchronized with Firebase for real-time updates and customization.

## Features

- **Seat-Based Color Display**: Shows blue for even seat numbers, red for odd seat numbers
- **Flexible Seat Input**: Supports multiple seat format inputs (e.g., "Section A, Row 5, Seat 12" or "A-5-12")
- **Full-Screen Experience**: Immersive color display with hidden system UI
- **Firebase Integration**: Real-time synchronization and remote configuration
- **Anonymous Authentication**: Automatic device tracking without user accounts
- **Dynamic Color Overrides**: Firebase-based color customization per seat

## User Manual

### Getting Started

1. **Installation**: Install the APK on your Android device (minimum Android 5.0)
2. **Launch**: Open the EventAnimation app
3. **Input Seat Information**: Enter your seat details in the input screen

### Using the App

#### Step 1: Enter Seat Information
When you first open the app, you'll see an input screen where you can enter your seat information.

**Supported Input Formats:**
- `Section A, Row 5, Seat 12`
- `A-5-12`
- `A5-12`
- `Section: A, Row: 5, Seat: 12`

#### Step 2: View Your Color
After entering valid seat information, the app will:
1. Parse your seat details
2. Calculate the appropriate color based on your seat number
3. Display the color in full-screen mode

#### Step 3: Full-Screen Color Display
The color display screen features:
- **Full-screen color background** based on your seat number
- **Hidden system UI** for immersive experience
- **Automatic color calculation**: Even seats = Blue, Odd seats = Red
- **Real-time updates** if colors are changed remotely

### Color Logic

The app uses the following priority system for color determination:

1. **Firebase Database Override** (Highest Priority)
   - Custom colors set for specific seats via Firebase
   - Format: `seatColors/{section}_{row}_{seat}: "#color"`

2. **Remote Config Colors** (Medium Priority)
   - Dynamic color themes from Firebase Remote Config
   - Keys: `default_blue_color`, `default_red_color`

3. **Default Calculated Colors** (Lowest Priority)
   - Even seat numbers: Blue (#0000FF)
   - Odd seat numbers: Red (#FF0000)

### Navigation

- **Back Button**: Return to seat input screen from color display
- **Re-enter Seat Info**: Use the back button to change your seat information
- **System Navigation**: Use Android's back gesture or button to navigate

### Error Handling

The app provides user-friendly error messages for:
- **Invalid seat format**: Clear instructions on supported formats
- **Network issues**: Graceful degradation when Firebase is unavailable
- **Missing seat information**: Prompts to enter required information

### Offline Usage

The app works offline with:
- **Default color calculation** (even/odd logic)
- **Cached remote config values**
- **Local seat parsing and validation**

### Firebase Features

#### Anonymous Authentication
- Automatically signs in users for device tracking
- No user accounts required
- Enables personalized experiences

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
- **Internet Connection**: Required for Firebase features (optional for basic functionality)
- **Storage**: Minimal storage requirements (< 10MB)
- **Permissions**: Internet access for Firebase synchronization

## Development Setup

### Prerequisites
- Android Studio 4.0 or higher
- Android SDK 21 or higher
- Firebase project with:
  - Authentication enabled
  - Realtime Database enabled
  - Remote Config enabled

### Building the App

```bash
# Clone the repository
git clone <repository-url>
cd EventAnimation

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

### Firebase Configuration

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
- Ensure Firebase configuration is properly set up
- Check that `google-services.json` is in the correct location
- Verify internet connection for Firebase initialization

### Colors Not Updating
- Check Firebase Remote Config settings
- Verify database permissions
- Ensure internet connectivity

### Seat Input Not Accepted
- Use supported seat format examples
- Check for typos in section/row/seat information
- Ensure all required fields are provided

## Support

For technical issues or questions:
1. Check the troubleshooting section above
2. Review Firebase console for configuration issues
3. Verify input format matches supported examples

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Version History

- **v1.0.0**: Initial release with basic seat-based color display
- **v1.1.0**: Added Firebase integration and remote configuration
- **v1.2.0**: Enhanced seat parsing and error handling
