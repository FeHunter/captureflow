# Roadmap

## Native foundation
- [x] Kotlin + Jetpack Compose project.
- [x] ViewModel with Coroutines/Flow.
- [x] Room persistence for captures and checklist tasks.
- [x] Share Intent intake for text, URLs and images.
- [x] Manual capture entry, search and category filters.
- [x] Complete/delete captures and tasks.
- [x] On-demand ML Kit OCR integration.
- [ ] Confirm Gradle build and unit tests in Android toolchain.
- [ ] Test share/OCR flows on emulator and physical devices.

## Product hardening
- [ ] Copy shared image media to app-private storage when URI grants are temporary.
- [ ] Add review/edit flow for capture metadata and OCR text.
- [ ] Show loading/error states for OCR and persistence failures.
- [ ] Associate checklist tasks with references.
- [ ] Add tests for URL normalization, deduplication and checklist rules.
- [ ] Improve accessibility, responsive layouts and visual polish.

## Future
- [ ] Best-effort screenshot discovery with clear permission/lifecycle behavior.
- [ ] Optional backup/sync behind explicit consent.
- [ ] Cloud AI or external task integrations after privacy review.

## Release gate
A milestone is complete only after code review, relevant tests, Gradle build verification and appropriate device testing.
