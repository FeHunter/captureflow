# CaptureFlow — Engineering Guidelines

## Product goal
Build an Android-first app that turns shared links, text and images into organized references, with persistent sub-checklists attached to each reference.

## Canonical stack
- Kotlin, Android SDK and Jetpack Compose / Material 3.
- MVVM with Android ViewModel, Coroutines and Flow/StateFlow.
- Room over SQLite for local persistence.
- Google ML Kit for on-device OCR.
- Gradle Kotlin DSL.
- Minimum Android API 26 unless a documented decision changes it.

The former Expo/React Native implementation is archived under `legacy-expo/`; do not add new product features there.

## Source of truth and architecture
- Active app source: `android-native/app/src/main/java/com/fehunter/captureflow/`.
- `MainActivity.kt`: Android entry point and Share/notification Intent dispatch.
- `ui/CaptureFlowApp.kt`: Compose UI for the single References screen, cards, fullscreen viewer and per-reference sub-checklist.
- `ui/CaptureViewModel.kt`: observable state and application actions.
- `data/CaptureDatabase.kt`: Room entities, DAOs, database and capture classification.
- `services/`: OCR, link metadata, private image storage and screenshot monitoring/notifications.
- Keep database access out of Composables; expose observable state through ViewModels.
- Prefer dependency direction: UI -> ViewModel -> service/use case -> repository/DAO.
- Keep domain rules independent from Compose and Android APIs wherever practical.

## Product and UI rules
- The main app experience is one screen: **Referências**. Do not reintroduce bottom navigation or a separate Tasks screen without an explicit product decision.
- A screenshot opened from the notification confirmation flow must be persisted as a `CaptureEntity` in References via `addImageCapture(..., category = "Referência")`; it must not create a standalone `ChecklistTaskEntity` in that flow.
- The sub-checklist belongs to a reference and is persisted separately via `capture_subtasks`.
- The fullscreen viewer supports horizontal swipe: left = next reference, right = previous; navigation follows the supplied/filtered capture list and does not wrap around.
- The fullscreen hint `‹ Deslize para navegar ›` is a visual overlay on the image area; it must not reserve layout height or alter `ContentScale.Fit`.
- The top bar theme menu offers system, light and dark modes.
- Current category filters are `Todas`, `Ler depois`, `Ideias`, `Tarefa`, `Importante`, `Mais tarde`. `Todas` is a filter, not a stored category. Review category consistency before changing labels.

## Capture and privacy rules
- Primary intake is Android Sharesheet / `ACTION_SEND` for text, URLs and images.
- Preserve received content and treat source app/source URL as optional unless verified.
- Distinguish user-provided content, OCR output, fetched metadata and inferred suggestions.
- Avoid duplicates for normalized textual content; do not deduplicate distinct image captures by generic placeholder text.
- OCR runs on-device. For image references, keep the original image and persist the extracted title, not raw OCR text in the reference content field.
- Shared images should be copied to app-private storage when possible. If copying fails, retain the original URI as fallback and handle later access failure without losing the capture.
- Persist captures and reference subtasks locally. Cloud sync is out of scope unless explicitly approved.
- Do not upload captured content without explicit consent.
- Screenshot discovery is optional and best-effort. It inspects MediaStore metadata/name/path, not screen pixels. Never use Accessibility Services or cross-app screen capture to infer screenshots.
- The optional notification monitor uses a foreground service. Keep it opt-in, provide a clear stop action and persistent notification, and explain image/notification permissions.
- Verify current Google Play policy for `specialUse` foreground services before public release.

## Testing and quality
- Add tests for classification, URL normalization, duplicate handling, OCR title heuristics and checklist/subtask rules.
- Handle unsupported share payloads, URI permission lifetimes, duplicates and OCR failures gracefully.
- Run `testDebugUnitTest`, `assembleDebug`, and appropriate device checks when available.
- Never claim tests/build passed unless the exact commit's result was verified.
- Check notifications, permissions, app lifecycle, deep links/Intents and private image deletion on real Android devices.
- Keep secrets and machine-specific paths out of Git.

## Documentation and Git
- Keep `docs/project-overview.md`, `docs/architecture.md`, feature docs and `docs/SD_captureflow.drawio` aligned with implementation.
- When a behavior changes, update functional contract, UI/UX, wireframes and changelog together.
- Mark unverified behavior and known inconsistencies explicitly rather than documenting assumptions as fact.
- Use concise conventional commits and reviewable pull requests.


## Fullscreen interaction rules
- Use Compose `HorizontalPager` for reference navigation: left advances, right goes back; preserve filtered order and do not wrap at the ends.
- Keep the fullscreen header synchronized with the settled pager page.
- **Editar título** is available only in fullscreen. Persist through `CaptureViewModel.updateCaptureTitle` and `CaptureDao.updateTitle`, changing only the title.
- Keep the sub-checklist entry point on the reference card; do not restore its shortcut in the fullscreen header.


## OCR, fullscreen e screenshots
- Ao ajustar a extração de título por OCR, considere a região central da tela como sinal prioritário, sem eliminar os sinais de perfil de interface, tamanho e qualidade do texto.
- No fullscreen, mantenha a proporção aproximada de 20% para cabeçalho e 80% para imagem. Títulos longos podem sobrepor a imagem com gradiente escuro para transparente, sem mudar o espaço reservado à imagem.
- Nunca exclua imagens escolhidas manualmente pelo usuário. A remoção da screenshot original só pode ser habilitada no fluxo de importação confirmado pela notificação, após salvar a referência e a cópia privada; tratar falhas de exclusão como não fatais, pois o Android pode exigir consentimento explícito.


## Tags, links compartilhados e layout
- Use exclusivamente as tags de captura `Ler depois`, `Verificar`, `Salvar` e `Importante` em filtros e seletores.
- Links recebidos via `ACTION_SEND` devem ser salvos mesmo quando a extração de metadados falhar. Tente extrair título, descrição e imagem Open Graph/Twitter; baixe imagens para armazenamento privado com timeout e limite de tamanho, sem bloquear o salvamento do link.
- O visualizador fullscreen reserva 20% do espaço ao cabeçalho e 80% à imagem; cartões compactos usam 272 dp de altura.


## Fullscreen contrast and native back navigation
- Keep the title overlay on the fullscreen image with a stronger dark gradient (near-opaque black behind the text, fading toward transparency); do not change the fullscreen header/image proportions, image `ContentScale.Fit`, or swipe pager behavior.
- Handle Android system Back (button or predictive/gesture back) in the active fullscreen layer: close the open capture viewer first, then return from an expanded category to the library. Never close the underlying category when only the viewer is dismissed.
- Keep the explicit close/back buttons. System Back is an additional native navigation path, not a replacement for visible controls.
- When a Compose `Dialog` has explicit `BackHandler` navigation, disable automatic `dismissOnBackPress` for that dialog to avoid competing dismissal paths.
