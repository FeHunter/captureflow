# Categories

Feature responsável pelo gerenciamento de categorias e pela UI de seleção/edição.

- `CategoriesViewModel`: estado, inclusão, renomeação e exclusão.
- `presentation/components`: diálogo de gerenciamento e seleção.
- Validação, inicialização de categorias padrão e regras de inclusão/renomeação/exclusão ficam em `ManageCategoriesUseCase`, usando o contrato público `CaptureRepository`.

A exclusão reatribui referências, categorias anteriores e registros legados dentro de uma transação. `Concluído` é uma categoria fixa.
