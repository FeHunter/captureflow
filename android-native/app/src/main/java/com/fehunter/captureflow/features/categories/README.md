# Categories

Feature responsável pelo gerenciamento de categorias e pela UI de seleção/edição.

- `CategoriesViewModel`: estado, inclusão, renomeação e exclusão.
- `presentation/components`: diálogo de gerenciamento, seleção de categoria e `CategoryFilterSection` (duas linhas de filtros e modal com todas as categorias).
- Validação, inicialização de categorias padrão e regras de inclusão/renomeação/exclusão ficam em `ManageCategoriesUseCase`, usando o contrato público `CaptureRepository`.

A exclusão reatribui referências e registros legados dentro de uma transação. Conclusão é um estado independente, não uma categoria.


## Filtro de categorias da biblioteca
- Todas as categorias são exibidas em duas linhas com rolagem horizontal independente; a distribuição alterna os itens entre a primeira e a segunda linha.
- O botão **Ver todas as categorias** permanece disponível e abre o diálogo com a lista completa.
