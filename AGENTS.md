# CaptureFlow — Agent Guidelines

## Product goal
Build an Android-first mobile app that turns shared links, text, images, and optionally detected screenshots into organized references and editable checklists.

## Stack
- React Native, Expo, TypeScript, and Expo Router.
- EAS Build for cloud builds.
- Native Android Kotlin modules only when a platform capability cannot be implemented reliably through Expo modules or supported packages.
- Keep dependencies compatible with the selected Expo SDK; use npx expo install for Expo-managed packages.

## Architecture
- Organize capabilities by feature under src/features/.
- Keep route files in app/ thin; move business logic into feature modules.
- Keep shared UI, design tokens, types, and utilities under src/shared/.
- Separate presentation, domain rules, and persistence/integration concerns.
- Document meaningful decisions in docs/ or feature-local documentation.

## Capture and privacy
- Treat source URL, source app, and extracted metadata as optional unless verified.
- Distinguish metadata received from a share intent, extracted via OCR, fetched from a public page, or inferred by AI.
- Never upload captured images or extracted personal content without explicit user-facing consent.
- Do not implement invasive cross-app monitoring or accessibility-service surveillance.
- Handle permission denial, background restrictions, duplicate screenshots, and unsupported share payloads gracefully.
- Prefer native Android share intents as the primary capture path; screenshot detection is complementary and best-effort.

## Quality
- Use strict TypeScript; avoid any unless documented.
- Add tests for parsing, URL normalization, deduplication, and checklist rules as features are implemented.
- Run lint, type checks, tests, and Expo diagnostics when the environment supports them.
- Never claim a build or test passed unless it was actually run.
- Keep secrets out of Git; commit placeholders only in .env.example.

## Git conventions
- Use concise conventional commit messages.
- Keep changes scoped and reviewable.
- Update README and docs when setup, architecture, or behavior changes.
