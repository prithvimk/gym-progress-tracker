# Gym Progress Tracker

A lightweight, offline-first Android app for logging workouts, tracking progressive overload, and staying consistent. Free and open source.

## Status

Phase 2 (workout logging) of the [v1 implementation plan](docs/IMPLEMENTATION_PLAN.md).

## Build

Requires JDK 17 and the Android SDK (Android Studio sets both up).

```
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew ktlintCheck lintDebug testDebugUnitTest
```

Every push builds a debug APK in GitHub Actions; download it from the run's artifacts.

## Dev builds

Every push to `main` also updates the [`dev` pre-release](../../releases/tag/dev) with the latest debug APK (`gym-tracker-dev.apk`) and a `version.json` holding its version code. CI sets `versionCode` to the workflow run number, so each build installs over the one before it.

Updates only install over a build signed with the same key, so CI signs with a fixed dev key stored in two repository secrets. To set them up once:

```
keytool -genkeypair -keystore dev.keystore -storetype PKCS12 -alias gymtracker-dev \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Gym Tracker Dev"
base64 -w0 dev.keystore    # on macOS: base64 -i dev.keystore
```

Then add `DEV_KEYSTORE_BASE64` (the base64 output) and `DEV_KEYSTORE_PASSWORD` (the password you chose) under Settings → Secrets and variables → Actions. Keep `dev.keystore` somewhere safe and out of the repo. Until both secrets exist, CI still builds but skips the dev release.

A build installed from Android Studio is signed with your machine's debug key, so uninstall it once before installing the first dev build. That clears the app's data.

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
