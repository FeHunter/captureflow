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


## Architecture pattern: MVVM (required)
- **MVVM is the standard architecture for all application features.** Do not introduce a different presentation architecture without an explicit project-level decision.
- Keep route files in `app/` thin: they should connect Expo Router to the feature's View and avoid business rules.
- **View** (`view/`): renders UI and forwards user actions to the ViewModel. It must not contain domain rules or access persistence directly.
- **ViewModel** (`view_models/`): owns presentation state, coordinates user actions and calls application services/use cases. It adapts domain results for the View.
- **Model/domain** (`model/`): contains domain entities, value objects, validation and business rules; keep it independent of React components.
- **Services** (`services/`): orchestrate application use cases and processing such as capture intake, content extraction, organization and OCR.
- **Repositories** (`repository/`): define data access contracts and implementations/adapters for persistence. Views and ViewModels must not access the database directly.
- Prefer dependency direction: `View -> ViewModel -> Service/use case -> Repository interface -> Repository implementation/storage`. Domain models and rules should not depend on UI or a specific storage provider.
- Shared reusable UI, design tokens, types and utilities belong in `src/shared/`; feature-specific components stay within their feature.
- Follow the feature folder's documentation before changing its implementation. Keep feature documentation close to the code (for example, `src/features/<feature>/doc/`) and record cross-feature/system decisions in `docs/`.

## MVP system decisions
- **Platform:** Android-first. Use React Native, Expo, TypeScript and Expo Router. Use a development build when native integration requires it.
- **Primary capture path:** Android native Share Intent from other apps. Support shared URLs, plain text and images where available. Screenshot detection outside the share flow is not an MVP dependency; if added later, it must be best-effort and privacy-preserving.
- **Capture intake:** validate incoming payloads, classify the content type, and preserve source information only when actually provided or verified. Source app, source URL and metadata are optional.
- **Processing:** extract available text and metadata, normalize URLs, and detect likely duplicates. Clearly distinguish received metadata, extracted content (including OCR), fetched metadata and AI-inferred suggestions. Never present inferred values as verified facts.
- **Automatic organization:** organize automatically whenever confidence is sufficient. Generate or suggest titles, categories/tags and checklist suggestions from available content. Do not require confirmation for every capture. If the content is ambiguous, processing fails, or confidence is insufficient, preserve the captured content and provide a clear review/edit path rather than inventing information or silently discarding it.
- **OCR:** run only when needed or requested, preferably on-device. Do not OCR every image by default. Keep OCR behind a service/interface so its implementation can be changed without coupling it to Views or domain rules.
- **Persistence:** local-first. Save references, metadata, categories/tags and checklist/task state in on-device storage. Store image/file data locally when needed. UI and domain logic must use repository contracts rather than depending directly on a storage engine.
- **Synchronization:** cloud accounts and cross-device synchronization are out of scope for the MVP. Keep persistence boundaries clean so a future sync service can be added without redesigning the feature/domain layer. Do not add cloud upload of captured content without explicit user-facing consent.
- **References and checklists:** users can browse, search/filter basic saved references, edit them and create/manage checklists with completable tasks. Attach checklists to references where relevant. Advanced AI-generated checklists and advanced search are future enhancements.
- **Privacy and resilience:** handle unsupported payloads, invalid URLs, permission denial, OCR failure and duplicate captures gracefully. Never implement invasive cross-app monitoring or accessibility-service surveillance.

## MVP capture flow
1. Receive shared content through the Android Share Intent adapter.
2. Validate the payload and identify whether it is a URL, text or image.
3. Extract available content and metadata; run OCR only when needed.
4. Normalize the content and check for likely duplicates.
5. Automatically organize it when confidence is sufficient; otherwise mark it for user review.
6. Persist the reference and any related local media/checklist data through repositories.
7. Show the result in the references/inbox UI; allow search, filtering, editing and checklist actions.

The flow is a logical guide, not a requirement that every capture opens a review screen. A successful, sufficiently confident capture should be saved with minimal interruption.

## System design documentation
- The current system design diagram is `docs/SD_captureflow.drawio`. Treat it as the current working diagram and update it as architecture decisions are made.
- Draw the system in clear layers: (1) external sources, (2) capture entry/validation, (3) application/domain services, (4) local persistence, and (5) user interface.
- Show the main components where relevant: Share Intent Adapter, Capture Intake, Payload Validator/Type Classifier, Content Processor, OCR Service, Auto Organizer, Duplicate Detector, Reference/Checklist Services, repository interfaces, local database and local file storage.
- Show decision points for invalid payloads, whether OCR is needed, and whether organization confidence is sufficient. Uncertain content should go to review; successful high-confidence content can be saved automatically.
- Keep future cloud synchronization/backend as a clearly marked future component, not an MVP dependency.
