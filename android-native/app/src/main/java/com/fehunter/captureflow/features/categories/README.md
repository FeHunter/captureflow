# Categories

Feature responsável pelo gerenciamento de categorias e pela UI de seleção/edição.

- `CategoriesViewModel`: estado, inclusão, renomeação e exclusão.
- `presentation/components`: diálogo de gerenciamento, seleção de categoria e `CategoryFilterSection` (duas linhas de filtros e modal com todas as categorias).
- Validação, inicialização de categorias padrão e regras de inclusão/renomeação/exclusão ficam em `ManageCategoriesUseCase`, usando o contrato público `CaptureRepository`.

A exclusão reatribui referências e registros legados dentro de uma transação. Conclusão é um estado independente, não uma categoria.


## Filtro de categorias da biblioteca
- Todas as categorias permanecem visíveis na view principal, distribuídas em duas linhas dentro de um único container com rolagem horizontal compartilhada; as duas linhas avançam juntas.
- O botão **Ver todas as categorias** permanece como atalho de UX e abre o diálogo com a lista completa, sem controlar a expansão/recolhimento da view principal.
