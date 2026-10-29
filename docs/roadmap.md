# Roadmap

## Milestone 0 — Repository scaffold
- [x] Public repository and initial project documentation.
- [x] Expo, TypeScript, and EAS configuration.
- [x] Basic welcome screen and initial domain types.
- [ ] Install dependencies and validate the scaffold with Expo diagnostics.

## Milestone 1 — Native share intake
- [ ] Receive shared plain text and URLs on Android.
- [ ] Receive image shares where provided by the source app.
- [ ] Normalize text, URL, and image payloads.
- [ ] Add a review screen and local persistence.
- [ ] Test with Chrome, YouTube, Instagram, and Gallery where available.

## Milestone 2 — Metadata and OCR
- [ ] Extract public page metadata safely.
- [ ] Add on-device OCR for images.
- [ ] Preserve field provenance and confidence.
- [ ] Add URL normalization and duplicate detection tests.

## Milestone 3 — Checklists
- [ ] Create and edit tasks manually.
- [ ] Attach tasks to captures.
- [ ] Search and filter the inbox.

## Milestone 4 — Screenshot discovery
- [ ] Evaluate Android gallery observation and lifecycle behavior.
- [ ] Design permission and notification UX.
- [ ] Test manufacturer-specific background restrictions.
- [ ] Keep detection optional and never make it the only capture path.

## Milestone 5 — Optional integrations
- [ ] Evaluate Supabase sync and backup.
- [ ] Evaluate Trello integration.
- [ ] Add cloud AI only behind explicit consent and clear data handling.

## Release gate
A milestone is complete only after code review, relevant tests, type checking, and a device/build verification appropriate to the changes.
