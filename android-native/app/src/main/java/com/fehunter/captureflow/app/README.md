# App

Responsabilidades de aplicação:
- `MainActivity`: entrada Android, permissões, intents e ciclo de vida de serviços.
- `di/AppContainer.kt`: composição manual de repositórios, gateways e ViewModel factories.
- `navigation/CaptureFlowNavigation.kt`: shell Compose, tema/ações globais e entrada da biblioteca.
- `navigation/CaptureLibraryCoordinator.kt`: composição entre Captures, Image Import, Categories, Search, Dashboard e Fullscreen. A orquestração cross-feature pertence ao App, não a uma feature.

A navegação/composição não deve conter regras de negócio ou acessar DAOs. A aplicação permanece em um módulo Gradle nesta etapa.
