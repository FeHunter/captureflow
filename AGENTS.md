# CaptureFlow — Engineering Guidelines

> **Fonte de verdade para agentes e contribuidores.** Estas regras orientam toda implementação, refatoração, revisão e documentação do Capture Flow. Em caso de conflito, preserve o comportamento observado no código e os contratos funcionais mais recentes; atualize este arquivo quando uma decisão arquitetural for aprovada. Não trate planos futuros como implementação já concluída.

## 1. Produto e stack canônica

- Produto Android-first para guardar links, textos e imagens como referências organizadas, com subtarefas persistentes vinculadas a cada referência.
- Stack ativa: Kotlin, Android SDK, Jetpack Compose / Material 3, Android ViewModel, Coroutines, Flow/StateFlow, Room/SQLite, Google ML Kit para OCR e Gradle Kotlin DSL.
- Minimum Android API 26, salvo decisão documentada em contrário.
- A implementação Expo/React Native em `legacy-expo/` é histórica; não adicionar novas funcionalidades nela.
- Persistência local. Sincronização em nuvem e upload de conteúdo capturado estão fora do escopo sem aprovação explícita.

## 2. Arquitetura obrigatória: Feature-first + MVVM + Clean Architecture pragmática

O aplicativo deve ser organizado por **funcionalidade (feature-first)**. Cada feature mantém suas responsabilidades de Presentation, Domain e Data, com MVVM na camada de apresentação.

### 2.1 Camadas e responsabilidades

- **Presentation:** telas e componentes Compose, estado de UI, eventos e ViewModels. Não contém SQL, acesso direto a DAO, IO ou regras de negócio complexas.
- **ViewModel:** recebe eventos da UI, coordena casos de uso, expõe estado imutável por StateFlow/Flow e executa operações assíncronas com lifecycle apropriado. Não conhece DAO nem implementações concretas de persistência.
- **Domain:** modelos de negócio, contratos de repositório e UseCases. Deve permanecer independente de Android, Compose, Room e frameworks externos sempre que possível.
- **Data:** implementação dos contratos, DataSources, entidades Room, DTOs, mappers e integrações necessárias. É a única camada de feature que conhece detalhes de persistência.
- **Core:** infraestrutura e componentes realmente compartilhados entre features. Não deve virar depósito genérico de regras de negócio ou serviços exclusivos de uma feature.
- **App:** ponto de entrada, composição das features, configuração global, navegação de alto nível e resolução de dependências.

Direção de dependência esperada:

```text
Presentation (Compose + ViewModel)
              |
              v
           UseCase
              |
              v
      Domain Repository contract
              ^
              |
      Data Repository implementation
              |
              v
        DataSource / Room / API
```

Domain não depende de Presentation ou Data. A implementação Data depende dos contratos Domain; Presentation depende de Domain, não de Room.

### 2.2 Estrutura-alvo de pacotes

A estrutura abaixo é o destino arquitetural. **Não pressupor que já esteja implementada**: a migração deve ser incremental e cada etapa deve compilar.

```text
android-native/app/src/main/java/com/fehunter/captureflow/
├── app/
│   ├── CaptureFlowApplication.kt
│   ├── MainActivity.kt
│   ├── CaptureFlowApp.kt
│   ├── navigation/
│   └── di/
│
├── core/
│   ├── database/       # RoomDatabase, configuração e migrations compartilhadas
│   ├── designsystem/   # tema, tokens e componentes realmente genéricos
│   ├── storage/        # abstrações/infraestrutura de armazenamento reutilizável
│   ├── android/        # integrações Android compartilhadas (intents/permissões etc.)
│   ├── navigation/     # contratos/helpers de navegação compartilhados, se necessários
│   └── common/         # Result, dispatchers e utilitários sem regra de feature
│
└── features/
    ├── captures/
    │   ├── data/
    │   │   ├── datasource/
    │   │   ├── mapper/
    │   │   └── repository/
    │   ├── domain/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── usecase/
    │   └── presentation/
    │       ├── components/
    │       ├── screens/
    │       ├── state/
    │       └── viewmodel/
    ├── imageimport/
    ├── search/
    ├── categories/
    ├── fullscreen/
    ├── checklists/
    ├── dashboard/
    └── settings/
```

Cada feature deve adotar a separação `data/`, `domain/` e `presentation/` quando houver responsabilidades correspondentes. Não criar arquivos/pastas vazios ou abstrações artificiais apenas para preencher o padrão. Componentes específicos permanecem dentro da feature proprietária.

### 2.3 Responsabilidade de cada feature

- **captures:** ciclo de vida das referências: criar, observar, consultar por identificador, editar, concluir/restaurar e excluir. É a proprietária do modelo e das regras de referência.
- **imageimport:** entrada de links/textos/imagens quando aplicável ao fluxo de importação, Photo Picker, compartilhamento, screenshots, OCR e estado de processamento. Entrega dados validados/enriquecidos para persistência por Captures; não duplica a lógica de persistência de referências.
- **search:** consulta, busca textual, combinação de filtros e apresentação dos resultados. Consome contratos de consulta de Captures; não acessa seu DAO ou banco diretamente.
- **categories:** regras e operações de categorias, incluindo validações, ordenação e reatribuição segura de referências quando uma categoria é removida.
- **fullscreen:** experiência Details Fullscreen, pager, sincronização da referência ativa, título, anotações e ações disponíveis no detalhe. Usa casos de uso/contratos de domínio, nunca DAO diretamente.
- **checklists:** subtarefas persistidas e vinculadas a uma referência por `captureId`. Não reintroduzir uma tela independente de tarefas sem decisão explícita de produto.
- **dashboard:** indicadores e estatísticas de referências/categorias. Somente consulta; não modifica Captures diretamente.
- **settings:** tema, preferências e configurações do aplicativo, incluindo a experiência de configuração de monitoramento/permissões quando apropriado.

A divisão final de um serviço entre features/core deve seguir sua responsabilidade real. Ex.: OCR é específico de Image Import; armazenamento genérico privado pode ser infraestrutura Core; monitoramento de screenshots pertence à integração de importação/Android. Não mover serviços indiscriminadamente para Core.

### 2.4 Comunicação e limites entre features

- Uma feature não pode importar pacotes internos de outra feature (por exemplo, `features.captures.data.*` ou `features.captures.presentation.*`).
- A comunicação ocorre por contratos públicos e mínimos de Domain, casos de uso expostos deliberadamente, resultados/eventos ou navegação com argumentos simples (preferencialmente IDs).
- Não passar DAO, Room Entity, ViewModel ou estado interno de uma feature para outra.
- Não criar dependências circulares. Se duas features precisarem compartilhar uma regra/modelo, avaliar se o contrato pertence ao domínio proprietário ou a um componente de domínio compartilhado explícito; não resolver criando imports cruzados.
- O `app/` atua como composition root e pode conhecer os pontos de entrada públicos das features para montá-las.
- Core não deve importar features. Features podem depender de Core.
- Evitar EventBus/global mutable state como mecanismo padrão de comunicação.

### 2.5 MVVM e estado de UI

- Cada fluxo/tela com estado e interação próprios deve ter ViewModel com responsabilidade delimitada.
- Usar `UiState` imutável (preferencialmente data class) e eventos/ações explícitos; expor estado via `StateFlow` e coletar lifecycle-aware na UI.
- Composables devem ser tão stateless quanto possível: recebem estado e callbacks. Estado puramente visual/local pode permanecer em Compose quando não representa regra ou dado de negócio.
- ViewModels não devem acumular responsabilidades de várias features.
- Não mover lógica para um UseCase sem benefício real: UseCases representam operações/regras de domínio identificáveis, não wrappers artificiais para cada chamada.
- Dispatchers e trabalho de IO devem ser tratados fora da UI; não bloquear a main thread.

### 2.6 DI e módulos Gradle

- Na primeira etapa, manter as features como pacotes no módulo Android existente.
- Não criar módulos Gradle por feature nem introduzir Hilt/Koin simultaneamente à migração sem decisão arquitetural explícita.
- Começar com um composition root/ `AppContainer` manual, interfaces e construção explícita de dependências.
- Avaliar módulos Gradle e DI automatizada somente após a separação por pacotes estar estável e existir benefício concreto de isolamento/build/testes.

## 3. Organização do código atual e migração

O código ativo fica em `android-native/app/src/main/java/com/fehunter/captureflow/`. A organização atual ainda pode conter `ui/`, `data/` e `services/`; isso é legado de estrutura durante a migração, não o destino arquitetural.

- Não fazer uma reescrita big-bang.
- Migrar incrementalmente, mantendo o aplicativo compilável e funcional após cada etapa.
- Ordem recomendada: (0) baseline/inventário; (1) app/core/composition root; (2) Captures como feature piloto; (3) Image Import; (4) Categories e Search; (5) Fullscreen e Checklists; (6) Dashboard e Settings; (7) limpeza e validação.
- Captures é a implementação de referência para estabelecer convenções reutilizáveis.
- Durante a transição, evitar duas implementações ativas da mesma regra. Remover código antigo somente depois de migrar chamadas e validar equivalência.
- Mover arquivos sem alterar comportamento sempre que possível; separar refatoração estrutural de mudanças de produto.
- Antes de alterar um fluxo, localizar seus chamadores, estados, persistência, integrações e documentação.

## 4. Composition root, Activity e navegação

- `MainActivity` permanece como entrada Android para launcher, Sharesheet/Intents e intents de notificação. Deve encaminhar payloads para o ponto de entrada apropriado, sem concentrar regras de negócio.
- `CaptureFlowApplication` (se/ quando introduzida) e `app/di` são responsáveis por criar e fornecer dependências compartilhadas.
- `CaptureFlowApp` compõe a experiência principal e integra pontos de entrada das features; não deve concentrar a implementação de todas as telas, diálogos e regras.
- Navegação de alto nível fica em `app/navigation`; navegação interna e estado visual específico podem pertencer à feature.
- Passar identificadores e argumentos serializáveis entre destinos; cada destino carrega seus dados por contratos/casos de uso.
- A experiência principal continua sendo uma tela de Referências; não reintroduzir bottom navigation ou tela independente de Tasks sem decisão explícita de produto.

## 5. Regras de produto e UI que devem ser preservadas

- Nome visível do produto: **Capture Flow** na top bar e no launcher.
- A experiência principal é a biblioteca de referências.
- Tags disponíveis nos filtros e seletores: `Ler depois`, `Verificar`, `Salvar` e `Importante`. `Todas` é filtro, não valor persistido.
- `Concluído` é uma categoria/estado especial do sistema: não pode ser atribuída manualmente a uma nova referência, renomeada ou excluída. Ao concluir, guardar a categoria anterior; ao reabrir, restaurá-la.
- A sub-checklist pertence a uma referência e é persistida em `capture_subtasks`, associada por `captureId`. Não restaurar uma tela independente de tarefas sem aprovação.
- Uma screenshot confirmada pelo fluxo de notificação deve ser persistida como referência na biblioteca, não como `ChecklistTaskEntity`.
- Preservar o comportamento de filtros, busca, categorias, progresso e conclusão documentado em `docs/current-state.md`; em caso de divergência, verificar código e registrar a inconsistência antes de alterar.
- Cartões compactos atualmente usam 272 dp conforme a diretriz visual registrada; não alterar dimensões sem validar o layout atual e a decisão de produto.

## 6. Captura, links, imagens e privacidade

- Entrada primária: Android Sharesheet / `ACTION_SEND` para texto, URLs e imagens, além do Photo Picker e fluxo opt-in de screenshot.
- Preservar conteúdo recebido. Tratar origem/app/URL de origem como opcional, salvo quando verificada.
- Diferenciar conteúdo fornecido pelo usuário, resultado de OCR, metadados obtidos da web e sugestões inferidas.
- Evitar duplicatas para conteúdo textual normalizado e URLs conforme contrato atual. Não deduplicar imagens distintas usando texto placeholder genérico.
- OCR é local/on-device. Para referências de imagem, manter a imagem como capa e persistir o título extraído; não gravar OCR bruto em `content` sem decisão explícita.
- Copiar imagens compartilhadas para armazenamento privado do app quando possível. Se falhar, manter URI original como fallback e tratar falha de acesso sem perder a referência.
- Nunca excluir imagem escolhida manualmente pelo usuário.
- Exclusão da screenshot original só pode ocorrer no fluxo de importação confirmado pela notificação, após salvar referência e cópia privada. Falhas de exclusão são não fatais e podem exigir consentimento explícito do Android.
- Não fazer upload de conteúdo capturado sem consentimento explícito. Cloud sync está fora do escopo.
- Descoberta de screenshots é opcional e best-effort: inspeciona metadados/nome/caminho do MediaStore, não pixels de outros apps. Nunca usar Accessibility Services ou captura de tela entre aplicativos para inferir screenshots.
- Monitoramento por notificação deve ser opt-in, ter ação clara para parar, notificação persistente e explicação de permissões de imagem/notificação.
- Antes de release público, verificar a política vigente do Google Play para foreground service `specialUse`.

### 6.1 Compartilhamento de links

- Um link recebido por `ACTION_SEND` deve entrar em estado pendente; não persistir em Room antes da escolha explícita de uma categoria válida entre `Ler depois`, `Verificar`, `Salvar` e `Importante`.
- Cancelar/fechar a confirmação descarta a captura pendente e não persiste.
- Buscar metadados HTTP(S) fora da main thread, com connect/read timeout e limite de tamanho de HTML.
- Prioridade de título: primeiro `H1`, depois Open Graph/Twitter/document title e host como fallback.
- Prioridade de imagem: imagem de conteúdo em `article`, depois `main`, depois metadados Open Graph/Twitter.
- Resolver URLs relativas usando a URL final após redirects. Não executar JavaScript.
- Se uma nova URL compartilhada substituir a pendente, ignorar respostas antigas (stale) da requisição anterior.
- Falha de metadados/rede não descarta a URL original: permitir salvar com título fallback.
- Baixar a imagem de capa somente após confirmação do usuário e armazenar privadamente quando possível.
- Não fazer requisição ilimitada nem baixar HTML/imagens sem limites.

## 7. Regras de fullscreen e navegação

- O visualizador fullscreen usa navegação horizontal com Compose `HorizontalPager`: swipe para esquerda avança; swipe para direita retorna. Respeitar a lista fornecida/filtrada e não fazer loop nas extremidades.
- O cabeçalho e as ações devem acompanhar a página efetivamente assentada (settled page), não apenas o índice solicitado.
- Manter proporção aproximada de 20% para cabeçalho e 80% para área de imagem.
- Imagem deve preservar `ContentScale.Fit`; a dica `‹ Deslize para navegar ›` é overlay na área da imagem e não pode reservar altura nem alterar a escala.
- Títulos longos podem sobrepor a imagem com gradiente escuro forte (quase opaco atrás do texto, dissolvendo para transparência), sem redimensionar a área reservada ao pager.
- Anotações não vazias devem aparecer no Details Fullscreen no canto inferior esquerdo, em painel preto translúcido com texto branco, até cinco linhas visíveis e reticências para conteúdo maior. O painel acompanha a referência ativa durante o swipe.
- A coluna de ações fullscreen permanece no canto inferior direito, conforme layout atual.
- **Editar título** está disponível no fullscreen e altera somente o título persistido; não modificar conteúdo, categoria ou demais campos.
- Anotações pertencem à referência; limite atual de 10.000 caracteres. Texto vazio limpa a anotação; cancelar não persiste.
- O acesso à sub-checklist permanece no cartão de referência; não restaurar atalho no cabeçalho fullscreen.
- Android system Back (botão/gesto/predictive back) fecha primeiro o visualizador ativo e só depois retorna de categoria expandida à biblioteca. Não fechar a categoria subjacente ao dispensar apenas o viewer.
- Manter controles explícitos de fechar/voltar; system Back é caminho adicional.
- Se um Compose Dialog tiver BackHandler explícito, desabilitar `dismissOnBackPress` automático para evitar duas rotas concorrentes de dismiss.

## 8. OCR e processamento de imagem

- Ao ajustar a extração de título por OCR, considerar a região central da tela como sinal prioritário, sem eliminar sinais de perfil de interface, tamanho, qualidade e conteúdo do texto.
- Manter heurísticas testáveis e independentes da UI sempre que possível.
- OCR vazio/falho não deve remover referência nem imagem; expor estado de processamento/erro de forma recuperável.
- Para referências de imagem, persistir título sugerido e manter conteúdo OCR bruto fora do campo de conteúdo, conforme contrato atual.
- Executar OCR fora da main thread e evitar processamento duplicado simultâneo para a mesma referência.

## 9. Persistência, Room e migrações

- Room/SQLite é a persistência local canônica atual.
- Entidades Room pertencem à camada Data (ou infraestrutura de database durante a migração); não expor entidades diretamente à Presentation ou a contratos públicos entre features.
- DAOs são acessados somente por DataSources/repositórios, nunca por Composables ou ViewModels.
- Não alterar esquema ou versão do banco apenas para reorganizar pacotes.
- Toda alteração real de esquema exige migration explícita, atualização de schema/contrato, testes de migração e preservação de dados existentes. Não usar destructive migration para contornar falhas.
- Antes de editar migrations, confirmar a versão declarada no código atual; não confiar em documentação possivelmente desatualizada.
- Ao excluir uma referência, remover subtarefas associadas e tratar a remoção de arquivo privado de forma segura; falha de limpeza de arquivo não deve corromper o banco.
- Atualizar `docs/features/captures/database_contract_current.md` e documentação relacionada quando entidades/DAOs/migrations mudarem.
- `ChecklistTaskEntity` e `checklist_tasks` são legado/compatibilidade até decisão explícita de remoção. Não reativar sua UI nem apagar dados durante a refatoração.

## 10. Testes e qualidade

- Adicionar testes unitários para regras de domínio, UseCases, normalização/classificação, URL normalization, deduplicação, heurísticas de título OCR, categorias, conclusão/restauração e subtarefas.
- Testar ViewModels com estados/eventos e dependências falsas quando possível.
- Testar mappers e repositórios; testar consultas e migrations Room.
- Testar UI Compose para interações/estados críticos e fluxos integrados de Android.
- Tratar payloads de share não suportados, permissões/tempo de vida de URI, duplicatas, respostas de rede antigas e falhas de OCR/armazenamento.
- Gates de validação quando ambiente disponível:
  ```bash
  ./gradlew testDebugUnitTest
  ./gradlew assembleDebug
  ```
- Executar também verificações em dispositivo para Sharesheet, Photo Picker, notificações, permissões, lifecycle, Back/gestos, pager e exclusão de arquivos privados.
- Não afirmar que build/testes passaram sem resultado verificado no commit exato.
- Não adicionar dependências, frameworks ou abstrações sem necessidade demonstrável.

### 10.1 Verificações arquiteturais

Conforme a migração avançar, criar/fortalecer verificações para garantir:

- Domain não importa Android, Compose, Room ou Data.
- Presentation não importa DAO, Room Entity ou implementação de repositório.
- Features não importam pacotes internos de outras features.
- Core não depende de features.
- Não existem ciclos de dependência.
- Composables não executam IO nem acessam persistência.
- Cada ViewModel tem responsabilidade delimitada.

## 11. Documentação e manutenção

Manter documentação alinhada com o código real:

- `AGENTS.md`: regras e fonte de verdade arquitetural.
- `docs/architecture.md`: visão arquitetural implementada e diagramas de dependência.
- `docs/current-state.md`: comportamento atualmente observado, sem descrever planos como fatos.
- `docs/project-overview.md`: visão do produto.
- `docs/SD_captureflow.drawio`: diagramas atualizados; a aba **Architecture by Features** representa a arquitetura-alvo até que a migração esteja concluída.
- `docs/features/{feature}/`: contrato funcional, arquitetura, acesso a dados/ações, UI/UX, testes e decisões relevantes da feature.

Para cada feature nova ou migrada, documentar:
- objetivo e responsabilidades;
- estrutura interna e ponto de entrada;
- modelos, contratos e UseCases;
- dependências permitidas e comunicação com outras features;
- fluxos funcionais e regras de negócio;
- testes e critérios de aceite;
- decisões e limitações conhecidas.

Quando um comportamento mudar, atualizar documentação funcional, UI/UX, wireframes/diagrama e changelog aplicáveis. Registrar divergências conhecidas e comportamentos não verificados explicitamente. Não declarar a arquitetura-alvo como concluída antes da migração e validação.

## 12. Git e segurança

- Manter secrets, tokens, credenciais e caminhos específicos de máquina fora do Git.
- Preferir commits convencionais e pequenos, com mudanças revisáveis.
- Separar refatorações estruturais de alterações de produto sempre que possível.
- Não sobrescrever alterações existentes sem inspecionar o estado atual e entender seu propósito.
- Revisar o diff e executar os gates aplicáveis antes de considerar uma etapa concluída.


## Estado da migração no código — branch feature-first

A primeira realocação de pacotes foi aplicada:
- `features/captures/data/local`: banco Room, entidades, DAOs e classificador separados em arquivos próprios.
- `features/captures/presentation/viewmodel`: ViewModel atual de Captures (ainda transitório).
- `features/captures/presentation/screens`: tela Compose atual (ainda agregada).
- `features/imageimport/data/{ocr,metadata,android}`: OCR, metadados e integrações Android de screenshots.
- `core/storage`: armazenamento privado compartilhado.
- `app/di/AppContainer.kt` e `CaptureViewModelFactory` fazem a composição manual das dependências; `MainActivity.kt` permanece na raiz do pacote como entrada Android.

A realocação de pacotes não equivale à conclusão de Clean Architecture: o ViewModel ainda acessa DAOs e serviços concretos, e a tela Compose ainda reúne múltiplas áreas do produto. Próximas mudanças devem extrair contratos Domain, repositórios, UseCases, ViewModels e telas por feature. Não adicionar novas responsabilidades ao ViewModel/tela transitórios. O schema Room permanece na versão declarada pelo código (8); esta etapa não deve criar migration.
