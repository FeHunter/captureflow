# Arquitetura atual — Capture Flow

**Atualizado:** 2026-11-04 | Room schema version 11.

Feature-first + MVVM e separação pragmática Presentation/Domain/Data em um único módulo Gradle; DI manual em app/di/AppContainer.kt.

## Ownership
- app/navigation: shell e composição; CaptureLibraryCoordinator integra Biblioteca.
- app/di: repositórios, gateways, UseCases e factories.
- core: contratos/utilitários/UI compartilhados.
- captures: agregado Capture, categorias de domínio, subtarefas, notas, mídias, Room e UseCases.
- imageimport: intents/Sharesheet, Photo Picker, câmera, metadados, OCR, screenshots e storage.
- imagecompression: compressão local.
- categories/search/dashboard: categorias, filtro e indicadores.
- fullscreen: detalhe paginado, ações e leitor web.
- youtubeplayer: WebView/IFrame e lifecycle.
- notes: editor; modelo/serialização pertencem a Captures.
- newsletters: feeds RSS/Atom, parser, fetcher, Room e leitor.
- settings: tema, ordenações, compressão, Privacidade e Tutoriais.
- backup: ZIP v2, restauração de imagens e mesclagem aditiva via contratos públicos de Captures/Newsletters; leitura compatível com v1.
- share: payload de domínio para compartilhamento, exportadores PDF/RTF e integração Android Sharesheet; não possui persistência própria.

## Dependências
Presentation consome Domain/UseCases/contratos; Data implementa contratos e acessa Room/Android. Domain não importa Android/Compose/Room/Data. Features não importam pacotes internos de Presentation/Data de outras features. Comunicação por contratos, callbacks/eventos ou IDs. Core não depende de features. Fullscreen não tem ViewModel próprio; Notes não duplica persistência; Backup delega lote ao repositório Room em transação.

## Persistência e validação
Room v11: captures, categories, capture_subtasks, capture_media, newsletter_sources, newsletter_articles e checklist_tasks legado. Share não adiciona tabelas. Não reativar UI autônoma de checklist. Um módulo Gradle e DI manual; não introduzir Hilt/Koin sem decisão. CI precisa validar testDebugUnitTest e assembleDebug no HEAD exato; testes não substituem validação Android real.
