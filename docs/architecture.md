# Architecture

## Product shape
CaptureFlow is an Android-first app that collects shared content and turns it into searchable references and user-editable checklists.

## Initial flow
1. Receive a shared URL, text, or image from Android's native share sheet.
2. Normalize the payload into a typed capture draft.
3. Resolve metadata from the received URL or image where possible.
4. Show a review step so users can correct the title, description, and source.
5. Save the capture and optionally attach checklist tasks.

## Layers
- app/: Expo Router route composition only.
- src/features/captures/: capture workflow and domain behavior.
- src/features/checklists/: task and checklist behavior.
- src/shared/: reusable types, design tokens, UI, and utilities.
- modules/: app-owned Expo native modules when required.
- docs/: architectural decisions, setup, and roadmap.

## Data provenance
Fields such as title, description, source app, and source URL must carry or otherwise preserve their provenance. A URL received directly through a share intent is different from a URL guessed from OCR or AI inference.

## Persistence
Start with a local-first approach. Choose the local persistence library after the first capture flow and offline requirements are prototyped. Cloud sync and Supabase are optional future capabilities, not prerequisites for the first milestone.

## Platform strategy
- Android: first-class share intent support and later best-effort screenshot discovery.
- iOS: share extension support should be evaluated separately; do not assume Android behavior maps directly to iOS.
- EAS Build: use development builds for native module work.
