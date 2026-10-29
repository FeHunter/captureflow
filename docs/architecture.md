# Arquitetura do CaptureFlow

## Stack canônica
Kotlin, Android SDK, Jetpack Compose/Material 3, ViewModel, Coroutines/Flow, Room/SQLite e Google ML Kit Text Recognition. O app canônico está em `android-native/`; `legacy-expo/` é histórico e não recebe novas features.

## Camadas e responsabilidades
- **Entrada Android — `MainActivity.kt`:** recebe intents do launcher, Android Sharesheet e notificação; interpreta texto/URL/URI e encaminha ao ViewModel/UI.
- **Apresentação — `ui/CaptureFlowApp.kt`:** tela única Referências, estado visual, busca, filtros, cartões, diálogo de importação, fullscreen e sub-checklist.
- **Orquestração — `ui/CaptureViewModel.kt`:** operações CRUD, Flow/StateFlow, deduplicação, importação de imagens, OCR, enriquecimento de links e subtarefas.
- **Dados — `data/CaptureDatabase.kt`:** entidades, DAOs, Room/SQLite e classificação/normalização.
- **Serviços — `services/`:** `OcrService` (ML Kit local), `LinkMetadataService` (HTML HTTP(S)), `SharedImageStorageService` (cópia/remoção de arquivos privados), `ScreenshotMonitor` e `ScreenshotNotificationService` (MediaStore e notificações).

```text
Share sheet / Photo Picker / Screenshot notification
                        |
                        v
                   MainActivity
                        |
                        v
          CaptureFlowApp (Compose UI)
                        |
                        v
                CaptureViewModel
                  /    |     \
                 v     v      v
              Room   OCR    Link metadata
               |    ML Kit   HTTP(S)
               v
     private image storage / MediaStore observer
```

## Fluxo de captura de texto/link
1. O Android entrega um Intent de compartilhamento.
2. `CaptureClassifier` normaliza/classifica o conteúdo.
3. O ViewModel evita duplicatas de texto normalizado e persiste `CaptureEntity`.
4. Para URL HTTP(S), `LinkMetadataService` tenta recuperar título/descrição de metadados HTML. Se falhar, mantém-se a referência original.

## Fluxo de imagem/OCR
1. Photo Picker ou Sharesheet fornece URI.
2. `SharedImageStorageService` tenta copiar a imagem para armazenamento privado.
3. O ViewModel insere a referência em Room.
4. `OcrService` reconhece texto no dispositivo e heurísticas visuais sugerem o título.
5. O título é atualizado em Room; a imagem original continua associada à referência. O OCR não deve apagar a referência se falhar.

## Sub-checklist
`CaptureSubtaskEntity` fica na tabela `capture_subtasks` com `captureId`. O ViewModel oferece inserir, concluir, remover e excluir os itens associados quando a referência é removida. A UI abre um `ModalBottomSheet` a partir do cartão ou do atalho fullscreen.

## Screenshot notification
O usuário ativa explicitamente `ScreenshotNotificationService`, que roda como foreground service e observa mudanças no MediaStore. O serviço busca imagens recentes por nome/pasta compatível e emite uma notificação. É detecção heurística e best-effort; não usa Accessibility Service nem captura a tela de outros apps. A confirmação na UI salva a URI como referência, apesar de algumas strings/nomes de ação ainda manterem linguagem histórica de tarefa — ver [visão geral e pendências](project-overview.md).

## Persistência e privacidade
- Banco local `captureflow.db`, Room; não existe sincronização cloud.
- Imagens são copiadas para armazenamento privado quando possível; a URI original pode ser fallback.
- OCR é local. Metadados de links exigem acesso de rede ao URL fornecido pelo usuário.
- Não adicionar banco/network dentro de Composables.
- Respeitar URI permissions, falhas de IO e apagar apenas arquivos que pertençam ao diretório privado do app.

## Regras de manutenção
Manter a direção UI → ViewModel → serviços/DAOs. Atualizar documentação e testes com mudanças em entidades, intents, OCR, navegação ou permissões. Nunca declarar build/testes aprovados sem verificar o commit correspondente.


## Fullscreen navigation and title editing

The fullscreen viewer uses Compose `HorizontalPager`, advances on swipe-left and returns on swipe-right, follows the filtered reference list, and does not wrap around. Keep the header synchronized with the settled page. The **Editar título** action exists only in fullscreen and updates `captures.title` through the ViewModel and a title-only DAO query. Keep sub-checklist access on the reference card, not in the fullscreen header.
