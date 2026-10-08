# Knife Tracker

A small Android app for logging the angles you sharpen your knives at.

## Features

- Keep a list of your knives with maker, steel, a target angle, and notes.
- Optionally attach a photo of each knife, taken with the camera or picked from the gallery.
  Photos are copied into the app's private storage.
- Log each sharpening session: angle per side, stones or method used, and notes.
  The dialog pre-fills the last angle and method you used for that knife.
- The knife list shows the most recent angle and date for every knife.

Everything is stored on the device with Room (SQLite). No account or network access is needed.

## Building

Requirements: JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install it with
`adb install app/build/outputs/apk/debug/app-debug.apk`, or open the project in Android Studio and press Run.

Every push to `main` and every pull request is built by GitHub Actions, and the debug APK is attached
to the workflow run as the `knife-tracker-debug-apk` artifact, so you can download it from the run's
Summary page and sideload it.

## Tech

Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room, and Coil for images. Min SDK 26 (Android 8.0).
