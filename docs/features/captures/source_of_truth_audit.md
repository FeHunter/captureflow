# Auditoria de fonte de verdade — Capture Flow

| Dado | Fonte autoritativa | Observações |
|---|---|---|
| Referências | Room `captures` via CaptureRepository | Agregado de Captures. |
| Categoria | `captures.category` e `categories` | Todas é filtro. |
| Conclusão | `captures.isCompleted` | Independente da categoria. |
| Subtarefas | `capture_subtasks` | Vinculadas por captureId; checklist_tasks é legado. |
| Mídias/capa | `capture_media` e compatibilidade `captures.imageUri` | Até 10 mídias por referência. |
| Anotações | `captures.notes` via CaptureNotesCodec | Blocos versionados; até 10.000 caracteres por bloco. |
| Newsletters | `newsletter_sources`, `newsletter_articles` | Persistência local. |
| Preferências | SharedPreferences via SettingsRepository | Tema, ordenações e compressão; fora do backup v1. |
| Backup | ZIP v1 com manifest.json e images/ | Artefato exportado; importação aditiva. |
| OCR | ML Kit + TitleDetectionService | Sugestão local, não verdade semântica garantida. |

Não há backend/sync remoto como fonte de verdade. checklist_tasks é legado.
