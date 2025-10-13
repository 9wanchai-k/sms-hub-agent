# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application named "sms-hub-agent" built with Kotlin and Jetpack Compose. The app uses Material 3 Design with Adaptive Navigation Suite for responsive UI across different device sizes.

**Package name**: `com.example.sms_hub_agent`
**Min SDK**: 35
**Target SDK**: 36
**Compile SDK**: 36

## Build System

The project uses Gradle with Kotlin DSL (`.kts` files) and version catalogs for dependency management.

### Common Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Run all unit tests
./gradlew test

# Run all instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.example.sms_hub_agent.ExampleUnitTest

# Lint checks
./gradlew lint

# Build and run all checks
./gradlew check
```

## Architecture

### UI Layer

The app uses **Jetpack Compose** with Material 3 components:
- Single Activity architecture (`MainActivity`)
- Navigation handled by `NavigationSuiteScaffold` for adaptive UI
- Theme defined in `ui.theme` package (Color, Type, Theme)

### Navigation Structure

Navigation is managed through an `AppDestinations` enum in `MainActivity.kt:71-78` with three destinations:
- HOME
- FAVORITES
- PROFILE

The adaptive navigation automatically switches between bottom bar, navigation rail, or drawer based on screen size.

### Dependency Management

Dependencies are managed through Gradle version catalogs (referenced via `libs` accessor). Key dependencies include:
- AndroidX Core KTX
- Lifecycle Runtime KTX
- Activity Compose
- Compose BOM (Bill of Materials)
- Material 3 and Material 3 Adaptive Navigation Suite

## Testing

### Unit Tests
Location: `app/src/test/java/com/example/sms_hub_agent/`
Runner: JUnit

### Instrumented Tests
Location: `app/src/androidTest/java/com/example/sms_hub_agent/`
Runner: AndroidJUnitRunner with Espresso

## Code Style

- Kotlin official code style (enforced via `gradle.properties`)
- Java 11 source/target compatibility
- JVM target: 11

## Key Files

- `app/src/main/java/com/example/sms_hub_agent/MainActivity.kt` - Main entry point and navigation setup
- `app/build.gradle.kts` - App module build configuration
- `build.gradle.kts` - Root project build configuration
- `settings.gradle.kts` - Project structure and repository configuration
