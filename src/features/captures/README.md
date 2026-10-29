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
