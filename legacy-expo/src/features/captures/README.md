# Captures feature

Owns the capture inbox, normalized incoming payloads, source metadata, and capture lifecycle.

Planned input sources:
- Android native share intents (text, URL, image).
- Manual image selection.
- Best-effort screenshot discovery on Android.

Rules:
- Keep source URL and source app optional.
- Preserve provenance for extracted and inferred fields.
- Deduplicate safely without discarding user-created items.
- Do not add background screenshot monitoring until permission, lifecycle, and device behavior are documented and tested.

## OCR (text extraction)

- OCR runs only when the user selects an image and taps **Extrair texto da imagem**.
- Uses `expo-text-extractor` (Google ML Kit on Android, Apple Vision on iOS) locally on the device; there is no upload to a backend.
- Extracted text is inserted into the editable capture content so the user can review it before saving.
- This native module requires a development build; Expo Go does not include it.


## Troubleshooting: native OCR module not found

Installing the npm package does not add its native code to an already-installed Android binary. After adding or updating `expo-text-extractor`, build and install the app again with `npx expo run:android` (or create a new EAS development build). Start Metro separately with `npx expo start -c`. The OCR adapter uses a lazy import so a stale binary does not crash the whole app on startup; it shows an actionable error when OCR is requested instead.
