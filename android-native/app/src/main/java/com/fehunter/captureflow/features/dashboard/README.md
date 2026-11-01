# Dashboard

Feature responsável pelo resumo do progresso da biblioteca.

- `ObserveDashboardSummaryUseCase` combina os fluxos de Captures e categorias.
- O indicador geral conta todas as referências com `isCompleted = true`.
- Cada categoria apresenta a quantidade concluída em relação ao total de referências naquela categoria.
- Os indicadores não dependem de busca, filtro de categoria ou preferência de ocultar concluídos.
- `DashboardViewModel` expõe o resumo reativo.
- `DashboardSummaryCard` apresenta o total concluído e a proporção concluídas/total por categoria.

Não duplicar persistência ou estado de Captures nesta feature.
