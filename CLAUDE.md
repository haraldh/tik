# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android development environment project using Nix flakes. It provides a reproducible development environment for Android app development with Kotlin and Java.

## Key Commands

### Environment Setup
```bash
# Enter the development environment (if not using direnv)
nix develop

# The environment automatically sets up:
# - ANDROID_HOME and ANDROID_SDK_ROOT
# - JAVA_HOME (JDK 17)
# - PATH with all Android SDK tools
```

### Android Development Commands
```bash
# Create a new Android project
gradle init --type basic --project-name MyApp

# Accept Android SDK licenses (required on first setup)
yes | sdkmanager --licenses

# Build an Android project
./gradlew build

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Connect to device/emulator
adb devices

# Start Android emulator
emulator -list-avds  # List available AVDs
emulator -avd <name> # Start specific AVD

# Mirror device screen
scrcpy

# Lint Kotlin code
ktlint
```

## Architecture & Structure

### Development Environment Configuration
- **flake.nix**: Core Nix flake configuration that defines:
  - Android SDK components (build tools 33.0.2 and 34.0.0, platforms 33 and 34)
  - Development tools (JDK 17, Gradle, Maven, Kotlin, CMake, Ninja)
  - Android tools (adb, emulator, scrcpy)
  - Environment variables and PATH setup

- **.envrc**: Enables automatic environment loading with direnv

### Android SDK Components
- Platform versions: Android 33 and 34
- Build tools: 33.0.2 and 34.0.0
- ABIs: x86_64 and arm64-v8a
- NDK: version 25.2.9519653
- Extras: Google Play services, Google repository

### Development Tools Available
- **Java/Kotlin**: JDK 17, Kotlin compiler, Kotlin language server
- **Build Tools**: Gradle, Maven, CMake, Ninja
- **Android Tools**: adb, sdkmanager, emulator, scrcpy
- **Code Quality**: ktlint
- **Version Control**: git

## Projects in this Repository

### magnifier-app/
An Android camera magnifier application with zoom and color inversion features. See `magnifier-app/README.md` for details.

Key commands for the magnifier app:
```bash
cd magnifier-app
./gradlew build           # Build the app
./gradlew installDebug    # Install on device/emulator
./gradlew clean          # Clean build artifacts
```

## Important Notes

- This is a development environment setup, not an Android application
- Android Studio is available but commented out in flake.nix (uncomment `android-studio` if needed)
- First-time setup requires accepting Android SDK licenses
- The environment supports both x86_64 and ARM64 architectures
- All tools are pinned to specific versions via flake.lock for reproducibility