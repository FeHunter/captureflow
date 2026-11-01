# Captures

Feature responsável pelo ciclo de vida das referências salvas.

## Estrutura
- `domain/model`: `Capture`, `CaptureCategory`, `CaptureSubtask`.
- `domain/repository`: contrato `CaptureRepository`.
- `domain/usecase`: operações de aplicação e regras de criação.
- `data/local`: entidades, DAOs e Room.
- `data/mapper`: conversão Room↔Domain.
- `data/repository`: implementação Room.
- `presentation/viewmodel`: estado e ações de Captures.
- `presentation/components`: cards de categoria e thumbnails reutilizáveis.
- A sub-checklist de cada referência é apresentada no fullscreen e permanece vinculada ao `captureId`.

O ViewModel consome `CaptureUseCases`; não deve importar entidades, DAOs ou implementações Data. A conclusão é independente da categoria: concluir/reabrir altera somente `isCompleted`. Os cartões exibem somente referências pendentes na prévia, mantendo a contagem de concluídas/total e a ação para abrir todos os itens. A categoria expandida permite alternar entre Pendentes e Concluídas. A coluna Room `previousCategory` permanece somente como legado de compatibilidade, sem uso na regra de negócio. A biblioteca é composta pelo `CaptureLibraryCoordinator` em `app/navigation`, que integra as features sem criar dependências entre suas camadas de Presentation. Categories, Search, Dashboard, Image Import e Fullscreen mantêm seus componentes e estados próprios.
