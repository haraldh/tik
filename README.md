./g# Magnifier App

A simple Android camera magnifier app with inverted color filter support. This app helps users magnify objects using their device's camera with zoom controls and an optional color inversion feature for enhanced visibility.

## Features

- **Camera Preview**: Real-time camera view using Android CameraX library
- **Zoom/Magnification**: Adjustable zoom from 1x to 10x using a slider control
- **Inverted Color Filter**: Toggle to invert colors for better contrast and visibility
- **Clean UI**: Minimalist dark theme with intuitive controls at the bottom of the screen
- **Portrait Mode**: Optimized for single-handed use in portrait orientation

## Requirements

- Android 7.0 (API level 24) or higher
- Device with camera
- Camera permission

## Building the App

### Prerequisites

1. Ensure you have the Nix development environment set up (see parent directory)
2. Enter the development shell:
   ```bash
   nix develop
   ```

### Build and Install

1. Navigate to the magnifier app directory:
   ```bash
   cd magnifier-app
   ```

2. Build the app:
   ```bash
   ./gradlew build
   ```

3. Install on connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
magnifier-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/magnifier/
│   │   │   └── MainActivity.kt       # Main app logic
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml # UI layout
│   │   │   ├── values/              # Colors, strings, themes
│   │   │   └── drawable/            # App icons
│   │   └── AndroidManifest.xml      # App configuration
│   └── build.gradle.kts             # App-level build config
├── build.gradle.kts                 # Project-level build config
└── settings.gradle.kts              # Gradle settings
```

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Camera Library**: AndroidX CameraX 1.3.1
- **Build System**: Gradle 8.2 with Kotlin DSL

## Usage

1. Launch the app
2. Grant camera permission when prompted
3. Use the zoom slider to adjust magnification (1x - 10x)
4. Toggle "Invert Colors" switch for inverted color mode
5. Point camera at object you want to magnify

## Permissions

The app requires the following permissions:
- `android.permission.CAMERA` - For camera access

## License

This project is dual-licensed under either:

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.
