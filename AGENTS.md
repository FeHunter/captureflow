# CaptureFlow — Engineering Guidelines

## Product goal
Build an Android-first app that turns shared links, text and images into organized references and editable checklists.

## Canonical stack
- Kotlin, Android SDK and Jetpack Compose.
- MVVM with Android ViewModel, Coroutines and Flow.
- Room over SQLite for local persistence.
- Google ML Kit for on-device OCR.
- Gradle Kotlin DSL.
- Minimum Android API 26 unless a documented decision changes it.

The former Expo/React Native app is archived under `legacy-expo/`; do not add new product features there.

## Architecture
- Active app source: `android-native/app/src/main/java/com/fehunter/captureflow/`.
- `ui/`: Compose screens and ViewModels. UI renders state and forwards user actions.
- `data/`: Room entities, DAOs, database and capture classification.
- `services/`: platform integrations such as OCR.
- `MainActivity`: Android entry point and Share Intent dispatch only.
- Keep database access out of Composables; expose observable state through ViewModels.
- Prefer dependency direction: UI -> ViewModel -> service/use case -> repository/DAO.
- Keep domain rules independent from Compose and Android APIs wherever practical.
- Add abstractions when complexity warrants them; avoid premature frameworks.

## Capture and privacy rules
- Primary intake is Android Sharesheet / `ACTION_SEND` for text, URLs and images.
- Preserve received content and treat source app/source URL as optional unless verified.
- Distinguish user-provided content, OCR output, fetched metadata and inferred suggestions.
- Organize automatically only when confidence is sufficient; otherwise preserve content for review.
- OCR runs on demand and on-device.
- Persist captures and checklist tasks locally. Cloud sync is out of MVP.
- Do not upload captured content without explicit consent.
- Screenshot discovery is optional, best-effort and privacy-preserving: observe MediaStore only while the app is foregrounded, request image-read permission explicitly, never use Accessibility Services, and do not run a background foreground-service solely for screenshot monitoring.
- Never use accessibility services to surveil other apps.

## Quality
- Add tests for classification, URL normalization, duplicate handling and checklist rules.
- Handle unsupported share payloads, URI permission lifetimes, duplicates and OCR failures gracefully.
- Run Gradle tests, build and device checks when available. Never claim they passed unless run.
- Keep secrets and machine-specific paths out of Git.

## Git and documentation
- Use concise conventional commits.
- Keep changes reviewable and update docs when stack or behavior changes.
- Keep `docs/SD_captureflow.drawio` aligned with the native architecture.


## Background screenshot notifications
- Keep screenshot monitoring opt-in. Explain image and notification permission requirements before activation.
- Use a foreground service only while explicitly enabled; expose a clear stop action and persistent notification.
- Never use Accessibility Services or screen-content capture to infer screenshots. Document OEM battery restrictions and screenshot-name/path heuristic limitations.
- Verify current Google Play policy for `specialUse` foreground services before any public store release.
