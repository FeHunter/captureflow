# Migration plan: Expo / React Native to native Android

## Decision
The canonical app is moving to Kotlin + Jetpack Compose. The previous Expo implementation is retained under `legacy-expo/` as reference only.

## Rationale
- Android Sharesheet is the primary capture entry point.
- Kotlin provides direct access to Android APIs and ML Kit without an Expo native-module bridge.
- The native project demonstrates Kotlin, Compose, MVVM, Room, Coroutines/Flow, Android intents and on-device OCR for the portfolio.
- The product is Android-first; this migration does not promise an iOS app.

## Status
- [x] Audit existing product goals, data model, local-first behavior and privacy rules.
- [x] Define native stack and package boundaries.
- [x] Add a native project with Compose UI, ViewModel, Room, share intake, OCR integration and basic checklists.
- [x] Update root engineering/product docs and system diagram.
- [x] Move previous Expo files under `legacy-expo/`.
- [ ] Run Gradle sync, unit tests and assembleDebug in a configured Android environment.
- [ ] Verify text, URL and image shares on emulator and physical device.
- [ ] Harden shared-image URI permission handling and OCR error states.
- [ ] Add editing/review flows, checklist-to-reference associations and accessibility polish.

## Principles
Preserve the product goal and privacy decisions, not the old framework. Keep content local, do not add cloud sync without consent, and keep screenshot discovery out of the initial migration. This is a native baseline, not a claim of full feature parity or release readiness.
