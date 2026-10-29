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
2. The activity forwards supported payloads, including image URIs from `EXTRA_STREAM` or `ClipData`.
3. Shared image content is copied to app-private storage when possible so it survives temporary URI permission grants.
4. The app normalizes and classifies the payload; text duplicates are avoided while distinct images are retained separately.
5. Content and the local image URI are saved in Room.
6. Users can search/filter, mark completion or delete references; deleting a capture also removes its private image file.
7. OCR runs on-device only when requested. The UI exposes progress and empty/error/success states, and recognized text is saved in Room.

## Privacy and resilience
- No cloud upload or cross-device sync is part of the MVP.
- Shared image URI permissions may be temporary or provider-specific. The app copies shared images to app-private storage before persisting them; if copying fails, it retains the original URI as a fallback and OCR may later fail.
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
