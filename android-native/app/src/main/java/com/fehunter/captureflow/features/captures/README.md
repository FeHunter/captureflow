# Feature: Captures

**Responsabilidade:** ciclo de vida das referências salvas pelo usuário.

## Organização
- `data/local`: Room, entidades, DAOs e migrations atuais (em decomposição).
- `domain`: destino para modelos de domínio, contratos de repositório e UseCases.
- `presentation`: telas Compose, estado e ViewModels.

## Estado da migração
A persistência e a apresentação existentes foram realocadas para esta feature. O `CaptureViewModel` permanece temporariamente como fachada legada e ainda coordena categorias, subtarefas e parte do fluxo de importação. Não adicionar novas responsabilidades a ele.

## Próximos passos
1. Separar entidades/DAOs e classificador.
2. Criar `Capture` como modelo de domínio e mapper.
3. Criar `CaptureRepository` e implementação Room.
4. Introduzir UseCases e retirar DAO do ViewModel.
5. Separar Search, Categories, Fullscreen e Checklists.
