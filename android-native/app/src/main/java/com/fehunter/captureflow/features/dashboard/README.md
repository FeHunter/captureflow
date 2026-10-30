# Dashboard

Feature responsável pelo resumo do progresso da biblioteca.

- `ObserveDashboardSummaryUseCase` combina os fluxos de Captures e categorias.
- `DashboardViewModel` expõe o resumo reativo.
- `DashboardSummaryCard` apresenta contagem de concluídos e distribuição por categoria.

Não duplicar persistência ou estado de Captures nesta feature.
