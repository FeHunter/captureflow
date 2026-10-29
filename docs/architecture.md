# Architecture

## Stack
CaptureFlow is Android-native and local-first: Kotlin, Jetpack Compose, ViewModel, Coroutines/Flow, Room/SQLite and Google ML Kit text recognition.

## Layers
- **Platform entry:** `MainActivity` receives launcher and Android Share Intent actions and forwards payloads to the ViewModel.
- **Presentation:** Compose screens render state; ViewModels coordinate actions and expose Flow state.
- **Domain rules:** payload classification, title/category suggestions and duplicate policy.
- **Services:** on-device OCR and future metadata extraction.
- **Persistence:** Room entities and DAOs over the local `captureflow.db`.
- **UI:** reference library, search, category filters and checklist.

## Capture flow
1. Another app shares text, a URL or an image with `ACTION_SEND`.
2. The activity forwards supported payloads.
3. The app normalizes and classifies the payload.
4. Content is saved locally while avoiding exact-content duplicates.
5. Users can search/filter, mark completion or delete references.
6. OCR runs only when requested for an image; extracted text is saved locally.

## Privacy and resilience
- No cloud upload or cross-device sync is part of the MVP.
- Shared image URI permissions may be temporary or provider-specific. Validate on real devices and copy media to app-private storage if durable access is required.
- Do not scan other apps' private data or use accessibility services to monitor them.
- Screenshot discovery is not implemented.

## Package layout
```
android-native/
  app/src/main/java/com/fehunter/captureflow/
    MainActivity.kt
    data/       # Room, DAOs and classification
    services/   # on-device OCR
    ui/         # Compose screens and ViewModels
```

Future synchronization must be opt-in and added behind repository boundaries without changing the local-first default.
