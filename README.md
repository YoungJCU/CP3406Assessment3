# PC Builder Lab

PC Builder Lab is a CP3406 educational Android application for university students learning how to plan a desktop PC. It is a guided simulator, not a shopping application: learners make component decisions, receive compatibility explanations and track their learning progress.

## Features

- Mission-based PC-building activities for programming, entry gaming and compact study builds.
- Plain-language feedback for CPU socket, DDR generation, motherboard/case size, GPU clearance, PSU headroom, budget and performance checks.
- Retrofit networking that reads the public `YoungJCU/buildpc-data` catalogue through the GitHub REST Contents API.
- Room persistence for results, progress, favourites and recent build history.
- DataStore settings for system/light/dark theme, text size and colour-blind palette.
- Jetpack Compose Material 3 UI, Navigation Compose, Hilt, MVVM and Repository pattern.
- Unit tests for the pure build-evaluation rules.

## Project setup

1. Push the separate `buildpc-data` directory to the public GitHub repository `YoungJCU/buildpc-data` on the `main` branch.
2. Open this `app` directory in Android Studio.
3. Let Gradle sync and run on an Android 8.0+ emulator or device with internet access.

The app needs internet only to load the learning catalogue. It never collects a login, location, contacts or analytics identifier. Progress is stored only in the local Room database.

## Testing

Run local unit tests with:

```bash
gradle testDebugUnitTest
```
