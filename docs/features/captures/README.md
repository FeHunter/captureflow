# Captures — Feature

Feature nativa responsável por receber, classificar, persistir, pesquisar e gerenciar referências e sub-checklists associadas a texto, URLs e imagens.

## Fonte de verdade
- Visão geral do produto/UI/fluxos: [../../project-overview.md](../../project-overview.md)
- Implementação: `android-native/app/src/main/java/com/fehunter/captureflow/`
- Entrada Android e intents: `MainActivity.kt`
- Banco e contratos Room: `data/CaptureDatabase.kt`
- Orquestração/estado: `ui/CaptureViewModel.kt`
- Interface: `ui/CaptureFlowApp.kt`
- OCR local: `services/OcrService.kt`
- Metadados de links: `services/LinkMetadataService.kt`
- Armazenamento de imagens: `services/SharedImageStorageService.kt`
- Monitoramento/notificações de screenshot: `services/ScreenshotMonitor.kt` e `services/ScreenshotNotificationService.kt`

## Documentos da feature
- [Contrato funcional](feature.md)
- [Arquitetura atual](architecture_current.md)
- [Contrato do banco](database_contract_current.md)
- [Acesso a dados e ações](data_access_and_actions.md)
- [Regras UI/UX](uiux_current.md)
- [Integrações](integrations.md)
- [Segurança e privacidade](security.md)
- [Wireframes](wireframes.md)
- [Auditoria de fonte de verdade](source_of_truth_audit.md)
- [Plano de testes](testing.md)
- [Changelog](CHANGELOG.md)

Os documentos devem descrever o código atual e identificar divergências conhecidas. Em caso de conflito, verificar o código da branch principal e atualizar todos os documentos relacionados antes de concluir a mudança.


## Estado atual e fluxos recentes
Consulte [Estado atual completo do app](../../current-state.md) para o inventário da arquitetura, layout e comportamento. Links compartilhados agora exigem seleção de categoria antes da persistência; a extração tenta H1 e imagem de article/main antes de usar metadados sociais como fallback.


## Melhorias aprovadas em 04/11/2026

- A Biblioteca oferece Fotografar, Galeria e Nova tarefa. A tarefa manual exige título e categoria existente; salva como texto sem imagem ou URL e não executa OCR/compressão.
- O modelo é criado por `CaptureFactory.createTask` e persistido pelo `CaptureViewModel`; Room/schema permanecem inalterados.
- `TaskPaperCover` em Core fornece capa pautada desenhada em Compose para cards e Fullscreen, sem arquivo placeholder.
- Na grade Concluídas, a mini-ficha inclui ação de exclusão com confirmação e reutiliza o fluxo existente de limpeza de arquivos associados.
- Animações locais e curtas foram aplicadas a carregamento de capa, mudança de status/progresso e saída de cards.
