## Migração aplicada — Feature-first + MVVM

A implementação atual está organizada por feature e segue a separação pragmática Presentation/Domain/Data:

- `features/captures`: modelos puros, `CaptureRepository`, `RoomCaptureRepository`, mappers e UseCases. `CaptureViewModel` consome os UseCases e não acessa Room.
- `features/imageimport`: `ImageImportGateway` independente de Android, implementação de OCR/metadados/storage em Data e ViewModel próprio para intents compartilhadas, links, imagens e OCR.
- `features/categories`: ViewModel de categorias, validação e componentes de gerenciamento.
- `features/search`: estado de consulta, filtro de domínio e campo Compose.
- `features/dashboard`: agregação reativa por UseCase, ViewModel e componente de resumo.
- `features/settings`: repositório SharedPreferences, UseCases e ViewModel do tema.
- `features/fullscreen`: componente Compose fullscreen.
- `app/di`: composição manual dos repositórios, gateways e factories.
- `app/navigation`: shell Compose e `CaptureLibraryCoordinator`, responsável por compor as features sem criar dependências Presentation→Presentation entre elas.
- `core/storage`: contrato mínimo `PrivateImageStorage`; a implementação de cópia/download/exclusão de imagens pertence a `features/imageimport/data/storage`.
- `core/ui`: carregamento de imagens com decodificação limitada.

A camada Domain não importa Android, Room ou Data. Presentation não importa entidades/DAOs Room nem implementações de serviços. `MainActivity` continua responsável por permissões Android e ciclo de vida; `SharedContentIntentParser` adapta intents Android na fronteira de Image Import.

## Limites conhecidos

A migração é feature-first no módulo Android atual; não houve extração de múltiplos módulos Gradle nem adoção de DI framework. `CaptureLibraryCoordinator` em `app/navigation` compõe as áreas da biblioteca; deve permanecer fino, delegando estado e regras aos ViewModels/use cases. A feature Captures não importa Presentation de outras features. O fullscreen recebe os dados de subtarefas de Captures por callbacks, sem criar uma segunda fonte de estado. Fullscreen não possui ViewModel próprio porque seu estado é controlado pelo fluxo de Captures. A tabela histórica `checklist_tasks` permanece apenas para compatibilidade do schema; subtarefas ativas pertencem a Captures.

Room continua na versão **8**. Nenhuma migration foi criada nesta refatoração.
