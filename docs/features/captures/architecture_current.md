# Arquitetura atual — Captures

## Stack
Kotlin, Android SDK, Jetpack Compose/Material 3, MVVM, Coroutines/Flow, Room/SQLite e ML Kit Text Recognition.

## Fluxo e dependências
```text
MainActivity (Android Intents)
        |
        v
CaptureFlowApp.kt (Compose UI)
        |
        v
CaptureViewModel.kt
   |        |          |
   v        v          v
Room/DAO  OcrService  LinkMetadataService
   |       ML Kit      HTTP(S)/HTML
   v
SharedImageStorageService
        ^
        |
ScreenshotNotificationService -> MediaStore / notification
ScreenshotMonitor -> MediaStore while foreground
```

## Responsabilidades
- **MainActivity:** recebe e encaminha payloads de share e URI/action da notificação.
- **CaptureFlowApp:** UI da tela única Referências, estado de pesquisa/categoria, cartões, seleção de imagem, modal de screenshot, detalhe fullscreen e sub-checklist.
- **CaptureViewModel:** expõe `captures`, `tasks`, `captureSubtasks` e `ocrStates`; coordena persistência, deduplicação, OCR e metadados.
- **CaptureDatabase:** entidades e DAOs Room.
- **OcrService:** reconhecimento local e heurística de título por geometria das linhas/perfis de UI.
- **LinkMetadataService:** busca metadados HTML de URL HTTP(S) compartilhada.
- **SharedImageStorageService:** tenta copiar imagens para diretório privado e remove somente arquivos privados reconhecidos.
- **ScreenshotNotificationService:** foreground service opt-in que observa MediaStore e emite notificação de screenshot.
- **ScreenshotMonitor:** observador separado de screenshot enquanto o app está visível.

## Diretrizes
Composables não devem acessar Room nem executar IO. Preservar conteúdo se enriquecimento/OCR falhar. Atualizar testes e documentação junto de mudanças de Intent, esquema Room, heurísticas ou layout.

## Pontos conhecidos
- Dados e métodos de `ChecklistTaskEntity` permanecem no ViewModel/banco, mas a tela atual não apresenta lista independente de tarefas.
- O fluxo da notificação salva screenshot como referência; algumas strings/ações internas ainda usam nomenclatura histórica de tarefa.
- A correção de imports Compose para `KeyboardOptions` e `ImeAction` foi integrada pelo PR #25; validar o CI do commit atual.


### Atualização de navegação e edição

O detalhe fullscreen utiliza `HorizontalPager` com sincronização do cabeçalho com a página ativa. O título principal pode ser editado no próprio fullscreen; a atualização usa `CaptureViewModel.updateCaptureTitle` e `CaptureDao.updateTitle`, sem alterar os demais dados da captura.


## Fullscreen pager and title correction

`CaptureFlowApp.kt` uses Compose Foundation `HorizontalPager` for fullscreen navigation. The pager is initialized at the selected reference, follows the filtered list, does not wrap around, and updates the header when the settled page changes. Title edits are routed through `CaptureViewModel.updateCaptureTitle` to the title-only `CaptureDao.updateTitle` query; no schema migration is needed. The fullscreen header no longer opens the sub-checklist; the card entry point remains.


## Ajustes de OCR e armazenamento de screenshots
- `OcrService` acrescenta uma pontuação de centralidade vertical ao ranqueamento de candidatos a título; heurísticas de perfil, tamanho e conteúdo continuam ativas.
- O fullscreen reserva `weight(0.30f)` ao cabeçalho e `weight(0.70f)` ao pager/imagem. Títulos longos são sobrepostos à imagem com `Brush.verticalGradient`, sem redimensionar a área do pager.
- `CaptureViewModel.addImageCapture` aceita `deleteSourceScreenshotAfterImport`; somente o fluxo iniciado pela notificação para adicionar a referência habilita esse comportamento. A exclusão da origem acontece após persistir a referência e apenas quando a cópia privada foi criada com sucesso.
- `SharedImageStorageService.deleteSourceScreenshot` tenta remover a URI `content://` original via `ContentResolver`; a tentativa é tolerante a falhas e não remove a cópia privada usada pelo app.
