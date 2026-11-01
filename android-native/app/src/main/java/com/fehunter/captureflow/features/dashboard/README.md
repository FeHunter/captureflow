# Dashboard

Feature responsável pelo resumo do progresso da biblioteca.

- `ObserveDashboardSummaryUseCase` combina os fluxos de Captures e categorias.
- O indicador geral conta todas as referências com `isCompleted = true`.
- Cada categoria apresenta uma barra horizontal proporcional à fração concluída, além das quantidades de concluídas e pendentes.
- Os indicadores não dependem de busca, filtro de categoria ou preferência de ocultar concluídos.
- `DashboardViewModel` expõe o resumo reativo.
- `DashboardSummaryCard` apresenta o total concluído e o progresso horizontal por categoria. Exibe cinco categorias inicialmente; quando existem mais, oferece ação inferior para expandir a lista completa e recolher novamente.

Não duplicar persistência ou estado de Captures nesta feature.
