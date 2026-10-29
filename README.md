# CaptureFlow

CaptureFlow is an Android-native app that turns shared links, text, and images into organized references and actionable checklists.

## Stack
- Kotlin + Android SDK + Jetpack Compose
- MVVM with ViewModel, Coroutines and Flow
- Room / SQLite for local-first persistence
- Android Sharesheet / ACTION_SEND for text, URLs and images
- Google ML Kit for on-device OCR
- Gradle Kotlin DSL

The former Expo/React Native implementation is retained under [legacy-expo/](legacy-expo/) for historical reference only. It is no longer the canonical app.

## Open and build
Requirements: Android Studio, Android SDK 36, JDK 17+ (JDK 21 is suitable), and network access for Gradle/Maven dependencies.

1. Clone the repository.
2. Open `android-native/` in Android Studio.
3. Allow Gradle sync to finish, then run the `app` configuration on an emulator/device.

From a terminal:
```powershell
cd android-native
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is generated at `android-native/app/build/outputs/apk/debug/app-debug.apk`.

## Native MVP baseline
- Manual text/link capture.
- Share text, URLs and images from other Android apps.
- Room persistence for references and checklist tasks.
- Search and category filters.
- Complete/delete references and tasks.
- On-demand, on-device OCR using ML Kit.

## Documentation
- [Engineering guidelines](AGENTS.md)
- [Architecture](docs/architecture.md)
- [Migration plan/status](docs/migration-plan.md)
- [Roadmap](docs/roadmap.md)
- [Capture feature](docs/features/captures.md)
- [Checklist feature](docs/features/checklists.md)
- [System diagram](docs/SD_captureflow.drawio)

## Privacy
MVP data stays on-device; no cloud sync, account, analytics or upload of captured content is included. Screenshot discovery is not implemented, and invasive cross-app monitoring is out of scope.

## Status
Native code and documentation have been added in the migration branch. Build and device validation are still pending; this is not yet release-ready.
