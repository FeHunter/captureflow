# Auditoria de fonte de verdade — Captures

| Dado | Fonte autoritativa | Observações |
|---|---|---|
| Versão exibida | Gradle `versionName` via `BuildConfig.VERSION_NAME` | Não duplicar literal na UI. |
| Referências | Tabela Room `captures` | Estado local. |
| Tarefas | Tabela Room `checklist_tasks` | Estado local. |
| Imagem original | `imageUri` persistida na entidade | Cópia privada preferida; URI original pode ser fallback. |
| Título/descrição de URL | Metadados HTML quando disponíveis | Fallback para conteúdo/URL original. |
| Título/descrição de screenshot | Heurística sobre linhas e bounding boxes do ML Kit | Sugestão automática, não verdade semântica garantida. |

Não há backend ou sync remoto como fonte de verdade para esta feature no MVP.
