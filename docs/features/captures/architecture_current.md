# Arquitetura atual — Captures

**Atualizado:** 2026-11-04.

Captures é proprietária do agregado Capture e persistência. A composição da Biblioteca fica em app/navigation.

- **Domain:** Capture, CaptureCategory, CaptureSubtask, CaptureMedia, CaptureNote, CaptureNotesCodec, CaptureRepository e UseCases.
- **Data:** entidades/DAOs Room, mappers e RoomCaptureRepository.
- **Presentation:** CaptureViewModel, estados de processamento e componentes CategoryCard, CategoryThumbnail, CompletedBadge e CreateTaskDialog.
- **App:** CaptureLibraryCoordinator conecta Captures a Categories, Search, Dashboard, Image Import, Fullscreen e Notes por estado/callbacks.

## Limites
- CaptureViewModel consome CaptureUseCases; não importa DAO, entidade Room ou implementação Data.
- Fullscreen não possui ViewModel próprio; recebe dados e callbacks de Captures.
- Notes apresenta/edita blocos, mas modelo/serialização pertencem ao agregado Capture.
- Backup usa UseCases públicos para exportar e importar lotes.
- checklist_tasks permanece por compatibilidade histórica; não existe tela independente de tarefas/checklists.
- Subtarefas ativas são capture_subtasks e pertencem a captureId.
- Room schema v11; detalhes em database_contract_current.md.
