# Search

Feature responsável pelo estado e apresentação da pesquisa na biblioteca.

- `domain/usecase/FilterCapturesUseCase.kt`: filtro puro por texto, categoria e estado de conclusão.
- `presentation/viewmodel/SearchViewModel.kt`: estado da consulta.
- `presentation/components/CaptureSearchField.kt`: campo de pesquisa reutilizável.

A pesquisa permanece local e reativa sobre o fluxo de Captures, sem criar uma segunda fonte de verdade.


## Posição na Biblioteca

O campo de pesquisa fica imediatamente abaixo da seção de filtros de categorias, mantendo os mesmos estados e critérios de filtragem. A ordenação visual é: ações de captura, dashboard, filtros de categorias, pesquisa e resultados.
