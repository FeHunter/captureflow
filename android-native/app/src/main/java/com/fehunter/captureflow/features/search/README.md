# Search

Feature responsável pelo estado e apresentação da pesquisa na biblioteca.

- `domain/usecase/FilterCapturesUseCase.kt`: filtro puro por texto, categoria e estado de conclusão.
- `presentation/viewmodel/SearchViewModel.kt`: estado da consulta.
- `presentation/components/CaptureSearchField.kt`: campo de pesquisa reutilizável.

A pesquisa permanece local e reativa sobre o fluxo de Captures, sem criar uma segunda fonte de verdade.
