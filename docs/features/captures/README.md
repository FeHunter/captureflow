# Captures — Feature

Feature nativa responsável por receber, classificar, persistir, pesquisar e gerenciar referências e tarefas derivadas de texto, URLs e imagens.

## Fonte de verdade
- Implementação: `android-native/app/src/main/java/com/fehunter/captureflow/`
- Banco e contratos Room: `data/CaptureDatabase.kt`
- Orquestração/estado: `ui/CaptureViewModel.kt`
- Interface: `ui/CaptureFlowApp.kt`
- OCR local: `services/OcrService.kt`
- Metadados de links: `services/LinkMetadataService.kt`
- Cópias de imagens: `services/SharedImageStorageService.kt`

## Documentos da feature
- [Contrato funcional](feature.md)
- [Arquitetura atual](architecture_current.md)
- [Contrato do banco](database_contract_current.md)
- [Acesso a dados e ações](data_access_and_actions.md)
- [Regras UI/UX](uiux_current.md)
- [Plano de testes](testing.md)
- [Changelog](CHANGELOG.md)

A documentação descreve o código observado. Pontos ainda não verificados em runtime estão identificados como pendentes.
