# Arquitetura do Capture Flow

## Direção arquitetural

A arquitetura-alvo do app Android é **Feature-first + MVVM + separação pragmática entre Presentation, Domain e Data**. As regras completas estão no `AGENTS.md`.

A migração está em andamento. Esta primeira etapa organiza os pacotes por propriedade funcional. A extração completa de contratos de Domain, repositórios, UseCases, ViewModels por feature e telas independentes ainda está pendente; não considerar a migração concluída.

## Stack canônica

Kotlin, Android SDK, Jetpack Compose/Material 3, Android ViewModel, Coroutines/Flow, Room/SQLite e Google ML Kit Text Recognition. App ativo em `android-native/`; `legacy-expo/` é histórico.

## Estrutura atual

```text
com.fehunter.captureflow/
├── MainActivity.kt
├── core/storage/SharedImageStorageService.kt
└── features/
    ├── captures/
    │   ├── data/local/CaptureDatabase.kt
    │   └── presentation/
    │       ├── screens/CaptureFlowApp.kt
    │       └── viewmodel/CaptureViewModel.kt
    └── imageimport/data/
        ├── android/{ScreenshotMonitor,ScreenshotNotificationService}.kt
        ├── metadata/LinkMetadataService.kt
        └── ocr/OcrService.kt
```

## Responsabilidades atuais e limites conhecidos

- `MainActivity`: entrada Android para intents, permissões e encaminhamento à UI.
- `features.captures.presentation.screens.CaptureFlowApp`: tela Compose existente; ainda reúne múltiplas experiências e deverá ser decomposta.
- `features.captures.presentation.viewmodel.CaptureViewModel`: ViewModel transitório; ainda coordena referências, categorias, subtarefas e parte da importação. Não adicionar novas responsabilidades.
- `features.captures.data.local.CaptureDatabase`: Room, entidades, DAOs, migrations e classificador atuais, ainda a decompor.
- `features.imageimport`: OCR, metadados HTTP(S), monitoramento e notificações de screenshots.
- `core.storage`: armazenamento privado reutilizável de imagens.

## Fluxo atual

```text
Sharesheet / Photo Picker / screenshot notification
                       |
                       v
                  MainActivity
                       |
                       v
         CaptureFlowApp (Compose)
                       |
                       v
       CaptureViewModel (transitional)
             |         |          |
             v         v          v
            Room   Image Import  Core Storage
```

## Fluxo de link compartilhado

1. `MainActivity` recebe `ACTION_SEND` e encaminha a intent.
2. O ViewModel cria um estado pendente e busca metadados fora da main thread; ainda não persiste a referência.
3. O usuário escolhe explicitamente uma tag válida.
4. Confirmar persiste a referência e tenta baixar a imagem de prévia para storage privado; cancelar não persiste.
5. Falha de metadados não descarta a URL original.

Prioridade de título: primeiro H1, depois Open Graph/Twitter/document title e host como fallback. Prioridade de imagem: conteúdo em `article`, depois `main`, depois metadados sociais. A extração é best-effort, limitada e não executa JavaScript.

## Dados e privacidade

- Banco local `captureflow.db`; versão Room declarada no código: **8**.
- `captures` contém referências; `capture_subtasks` guarda subtarefas por `captureId`; `checklist_tasks` permanece legado/compatibilidade.
- Sem sincronização cloud. OCR é local.
- O monitor de screenshots é opt-in e usa metadados MediaStore; não usa Accessibility Service nem lê pixels de outros apps.
- Importação manual nunca apaga a imagem original. Limpeza da screenshot original só ocorre no fluxo confirmado pela notificação e após cópia privada bem-sucedida.

## Próximas etapas

1. Extrair modelos de domínio e contratos de repositório de Captures.
2. Implementar DataSources/RepositoryImpl e UseCases, removendo acesso a DAO do ViewModel.
3. Separar Image Import em fluxo/ViewModel próprio e expor resultado para Captures por contrato público.
4. Extrair Categories, Search, Fullscreen, Checklists, Dashboard e Settings.
5. Decompor a tela Compose monolítica e criar testes dos limites arquiteturais.

## Qualidade

- Composables não acessam DAOs.
- Não persistir links antes da confirmação da tag.
- Não usar destructive migration.
- Executar `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` em ambiente Android configurado; não declarar sucesso sem execução verificada.
