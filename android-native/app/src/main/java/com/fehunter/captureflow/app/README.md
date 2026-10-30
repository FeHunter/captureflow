# App composition root

Ponto de entrada e composição da aplicação: navegação de alto nível e resolução de dependências.

- `app/di/AppContainer.kt` constrói o banco e serviços compartilhados e fornece a factory do ViewModel.
- `CaptureViewModelFactory` injeta as dependências no ViewModel sem framework externo.
- `MainActivity` permanece temporariamente na raiz do pacote como entrada Android para intents e permissões; movê-la em etapa isolada, validando os fluxos Android.

Manter a composição explícita e pequena. Não colocar regras de negócio no AppContainer.
