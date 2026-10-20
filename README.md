# Gym Progress Tracker

A lightweight, offline-first Android app for logging workouts, tracking progressive overload, and staying consistent. Free and open source.

## Status

Phase 0 (app skeleton and CI) of the [v1 implementation plan](docs/IMPLEMENTATION_PLAN.md).

## Build

Requires JDK 17 and the Android SDK (Android Studio sets both up).

```
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew ktlintCheck lintDebug testDebugUnitTest
```

Every push builds a debug APK in GitHub Actions; download it from the run's artifacts.

## v1 scope

- Log workouts: exercises, sets, weight, reps, optional RPE
- See what you lifted last time beside every set
- Routines and a rest timer
- History, calendar and automatic PR detection
- Export and import (JSON, CSV); no account, no backend

## Stack

Kotlin, Jetpack Compose, Room. Builds with Gradle and GitHub Actions.

## License

[MIT](LICENSE)
