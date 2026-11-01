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

- **Presentation:** telas e componentes Compose, estado de UI, eventos e ViewModels. Não contém SQL, acesso direto a DAO, IO de rede/persistência ou regras de negócio complexas. Decodificação local de bitmap é permitida somente via `core/ui`, em dispatcher de background.
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

### 2.2 Estrutura aplicada de pacotes

```text
android-native/app/src/main/java/com/fehunter/captureflow/
├── MainActivity.kt                  # entrada Android, permissões e lifecycle
├── app/
│   ├── di/AppContainer.kt            # composição manual das dependências
│   └── navigation/                   # shell + composição cross-feature
├── core/
│   ├── storage/                      # PrivateImageStorage + implementação
│   └── ui/                           # carregamento limitado de bitmaps
└── features/
    ├── captures/                     # Domain, Room Data, UseCases, ViewModel e biblioteca
    ├── imageimport/                  # gateway, parser Android, OCR, metadata e ViewModel
    ├── categories/                   # ViewModel e componentes de categorias
    ├── search/                       # filtro, estado e campo de pesquisa
    ├── fullscreen/                   # diálogo e página fullscreen
    ├── dashboard/                    # resumo, UseCase e ViewModel
    └── settings/                     # preferências, UseCases e ViewModel
```

### 2.3 Responsabilidade de cada feature

- **captures:** ciclo de vida das referências: criar, observar, consultar por identificador, editar, concluir/restaurar e excluir. É a proprietária do modelo e das regras de referência.
- **imageimport:** entrada de links/textos/imagens quando aplicável ao fluxo de importação, Photo Picker, compartilhamento, screenshots, OCR e estado de processamento. Entrega dados validados/enriquecidos para persistência por Captures; não duplica a lógica de persistência de referências.
- **search:** consulta, busca textual, combinação de filtros e apresentação dos resultados. Consome contratos de consulta de Captures; não acessa seu DAO ou banco diretamente.
- **categories:** regras e operações de categorias, incluindo validações, ordenação e reatribuição segura de referências quando uma categoria é removida.
- **fullscreen:** experiência Details Fullscreen, pager, sincronização da referência ativa, título, anotações e ações disponíveis no detalhe. Usa casos de uso/contratos de domínio, nunca DAO diretamente.
- **captures:** é proprietária das referências e das subtarefas persistidas em `capture_subtasks`, vinculadas por `captureId` e apresentadas no fullscreen. A entidade/DAO histórica `checklist_tasks` permanece apenas para compatibilidade do schema Room, sem feature ou tela ativa.
- **dashboard:** indicadores e estatísticas de referências/categorias. Somente consulta; não modifica Captures diretamente.
- **settings:** tema, preferências e configurações do aplicativo, incluindo a experiência de configuração de monitoramento/permissões quando apropriado.

A divisão final de um serviço entre features/core deve seguir sua responsabilidade real. Ex.: OCR, download de capas, cópia de imagens compartilhadas e exclusão best-effort de screenshots pertencem a Image Import; Core mantém apenas o contrato mínimo de armazenamento privado usado por Captures. Não mover serviços indiscriminadamente para Core.

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
- Dispatchers e IO de rede/persistência devem ser tratados fora da UI; nunca bloquear a main thread. A única exceção de UI é decodificação local e limitada de imagens via `core/ui`, sempre em dispatcher de background.

### 2.6 DI e módulos Gradle

- Na primeira etapa, manter as features como pacotes no módulo Android existente.
- Não criar módulos Gradle por feature nem introduzir Hilt/Koin simultaneamente à migração sem decisão arquitetural explícita.
- Começar com um composition root/ `AppContainer` manual, interfaces e construção explícita de dependências.
- Avaliar módulos Gradle e DI automatizada somente após a separação por pacotes estar estável e existir benefício concreto de isolamento/build/testes.

## 3. Organização do código atual e migração

O código ativo fica em `android-native/app/src/main/java/com/fehunter/captureflow/` e a migração inicial Feature-first + MVVM foi aplicada nos pacotes descritos acima. `legacy-expo/` é histórico e não deve receber novas funcionalidades.

- Preservar o comportamento observado e manter o app compilável após cada mudança.
- A migração dos principais fluxos foi concluída estruturalmente; novas mudanças devem seguir os limites de Domain/Data/Presentation já estabelecidos.
- Captures é a feature de referência para convenções de modelos, repositórios, UseCases, ViewModels e testes.
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
- A conclusão é um estado independente da categoria (`isCompleted`). Concluir/reabrir uma referência não altera sua categoria; não criar nem usar uma categoria especial `Concluído`. O Dashboard e os cartões calculam pendentes/concluídas dentro de cada categoria. No Dashboard, exibir progresso horizontal concluído/total e quantidades explícitas de concluídas e pendentes; limitar a lista inicial a cinco categorias, com expansão/recolhimento no rodapé quando houver mais.
- Na biblioteca, o filtro de categorias exibe todas as categorias distribuídas em duas linhas com rolagem horizontal independente e mantém o modal **Ver todas as categorias** como alternativa. Em `Todas`, renderizar um cartão de categoria somente se houver ao menos uma referência pendente; com uma categoria específica selecionada, manter o cartão mesmo sem pendentes e explicar o estado vazio da prévia.
- Referências concluídas exibidas em thumbnails e no fullscreen devem usar o componente reutilizável `CompletedBadge` da feature Captures, sobreposto à imagem no fullscreen. Não duplicar estilos/selo localmente em cada card.
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
- Prioridade de metadados para YouTube: consultar primeiro o endpoint oficial oEmbed (`youtube.com/oembed`) para obter título e thumbnail; se falhar, manter a extração HTML genérica como fallback.
- Para os demais sites, prioridade de título: primeiro `H1`, depois Open Graph/Twitter/document title e host como fallback.
- Prioridade de imagem: imagem de conteúdo em `article`, depois `main`, depois metadados Open Graph/Twitter.
- Resolver URLs relativas usando a URL final após redirects. Não executar JavaScript.
- Se uma nova URL compartilhada substituir a pendente, ignorar respostas antigas (stale) da requisição anterior.
- Falha de metadados/rede não descarta a URL original: permitir salvar com título fallback.
- Baixar a imagem de capa somente após confirmação do usuário e armazenar privadamente quando possível.
- A câmera própria deve permitir foco por toque no preview e oferecer revisão/recorte antes de encaminhar a foto ao OCR. O recorte não deve persistir a captura automaticamente; a confirmação de categoria/título continua sendo obrigatória.
- Não fazer requisição ilimitada nem baixar HTML/imagens sem limites.

## 7. Regras de fullscreen e navegação

- O visualizador fullscreen usa navegação horizontal com Compose `HorizontalPager`: swipe para esquerda avança; swipe para direita retorna. Respeitar a lista fornecida/filtrada e não fazer loop nas extremidades.
- O cabeçalho e as ações devem acompanhar a página efetivamente assentada (settled page), não apenas o índice solicitado.
- Manter proporção aproximada de 20% para cabeçalho e 80% para área de imagem.
- Imagem deve preservar `ContentScale.Fit`; a dica `‹ Deslize para navegar ›` é overlay na área da imagem e não pode reservar altura nem alterar a escala.
- Títulos longos podem sobrepor a imagem com gradiente escuro forte (quase opaco atrás do texto, dissolvendo para transparência), sem redimensionar a área reservada ao pager.
- O acesso às anotações fica no canto inferior esquerdo do fullscreen, com ícone de balão de conversa; abre o editor da feature `notes`, em modal amplo. A edição salva até 10.000 caracteres; cancelar não persiste.
- A coluna de ações fullscreen permanece no canto inferior direito. Para referências pendentes, manter checkbox, sub-checklist, link (quando válido), editar e excluir; para referências concluídas, exibir somente o checkbox para reabri-las. O atalho de anotações no canto inferior esquerdo permanece independente dessa coluna.
- O modal **Editar título** no fullscreen permite editar o título e selecionar outra categoria no mesmo formulário. Persistir cada campo pela operação correspondente de Captures, sem alterar conteúdo, URL ou imagem.
- Quando `sourceUrl` for HTTP(S) válida, o atalho para abrir o link permanece no cabeçalho e também aparece na coluna vertical de ações fullscreen.
- Anotações pertencem à referência; limite atual de 10.000 caracteres. Texto vazio limpa a anotação; cancelar não persiste.
- O acesso à sub-checklist permanece no cartão de referência; não restaurar atalho no cabeçalho fullscreen.
- Android system Back (botão/gesto/predictive back) fecha primeiro o visualizador ativo e só depois retorna de categoria expandida à biblioteca. Não fechar a categoria subjacente ao dispensar apenas o viewer.
- Manter controles explícitos de fechar/voltar; system Back é caminho adicional.
- Se um Compose Dialog tiver BackHandler explícito, desabilitar `dismissOnBackPress` automático para evitar duas rotas concorrentes de dismiss.

## 8. OCR e processamento de imagem

- OCR é executado localmente pelo ML Kit em Image Import. `OcrService` pertence a Data e adapta resultados da plataforma; `TitleDetectionService` pertence a Domain e mantém a seleção de título pura, testável e sem dependências Android.
- Para escolher o título, combinar altura relativa, área da caixa (largura × altura), largura com peso menor, posição vertical/central, perfil aproximado da interface, forma textual e confiança do OCR quando disponível.
- A confiança igual a zero deve ser tratada como indisponível/neutra, não como falha do reconhecimento.
- Agrupar linhas acima/abaixo da linha âncora somente quando distância, altura e alinhamento forem compatíveis; manter os demais textos na descrição.
- Filtrar ruído de interface com regras específicas e conservadoras. Não descartar manchetes apenas por conterem palavras genéricas como "share", "views" ou "more".
- ML Kit não expõe um atributo confiável de peso tipográfico/bold. Não afirmar que negrito foi detectado; altura e área são apenas proxies de destaque. Qualquer análise de pixels/OpenCV exige validação e justificativa antes de adicionar dependência.
- Considerar a região central como sinal prioritário, sem eliminar títulos legítimos próximos ao topo.
- OCR vazio/falho não deve remover referência nem imagem; expor estado de processamento/erro de forma recuperável.
- Para referências de imagem, persistir título sugerido e manter conteúdo OCR bruto fora do campo de conteúdo, conforme contrato atual.
- Executar OCR fora da main thread e evitar processamento duplicado simultâneo para a mesma referência.
- Manter casos de regressão e critérios de seleção em `docs/features/image-import/ocr-title-detection.md`; executar testes unitários do detector sem depender do ML Kit.

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

- Adicionar testes unitários para regras de domínio, UseCases, normalização/classificação, URL normalization, deduplicação, heurísticas de título OCR, categorias, conclusão independente da categoria, indicadores por categoria e subtarefas.
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
- Composables não acessam persistência nem rede. Decodificação local e limitada de bitmap é permitida via `core/ui`, com trabalho em dispatcher de background.
- Cada ViewModel tem responsabilidade delimitada.

## 11. Documentação e manutenção

Manter documentação alinhada com o código real:

- `AGENTS.md`: regras e fonte de verdade arquitetural.
- `docs/architecture.md`: visão arquitetural implementada e diagramas de dependência.
- `docs/current-state.md`: comportamento atualmente observado, sem descrever planos como fatos.
- `docs/project-overview.md`: visão do produto.
- `docs/SD_captureflow.drawio`: diagramas de referência; a aba **Architecture by Features** representa a organização feature-first aplicada, mantendo explícito que o app continua em um único módulo Gradle.
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


## Atualização da migração Feature-first + MVVM — 30/10/2026

A branch `refactor/complete-feature-architecture` avançou a migração para as features abaixo. Este checkpoint substitui as observações anteriores que diziam que o ViewModel principal ainda acessava DAOs ou que a tela raiz pertencia a Captures.

- **Captures:** modelos de domínio puros, contrato `CaptureRepository`, implementação Room, mappers, UseCases e `CaptureViewModel` sem referências a entidades/DAOs/Room.
- **Image Import:** contrato de gateway sem tipos Android, implementação Android em Data e `ImageImportViewModel` próprio para compartilhamento, metadados, importação de imagens e OCR.
- **Categories:** estado e operações em `CategoriesViewModel`; componentes Compose extraídos.
- **Search:** estado em `SearchViewModel`, filtro puro em UseCase e campo Compose independente.
- **Dashboard:** agregação em UseCase, estado em `DashboardViewModel` e componente Compose próprio.
- **Settings:** persistência em `SettingsRepository`, UseCases e `SettingsViewModel`.
- **Checklists:** a tabela histórica `checklist_tasks` permanece apenas como compatibilidade de schema dentro de Captures Data; subtarefas ativas são parte de Captures.
- **Fullscreen:** composable extraído para sua feature.
- **App/Core:** composição de dependências em `app/di`, shell Compose em `app/navigation` e carregamento de bitmaps em `core/ui`.

A Presentation consome modelos de domínio, UseCases e contratos de gateway. Entidades Room e DAOs ficam em Data. O domínio não deve importar Android, Room ou implementações Data. `MainActivity` permanece como entrada Android e trata permissões/intenções de plataforma.

Limites remanescentes conhecidos:
- O app continua em um único módulo Gradle, conforme a estratégia incremental do projeto.
- `CaptureLibraryCoordinator` em `app/navigation` coordena a composição cross-feature da biblioteca; não adicionar novas regras de negócio nela. As features não devem importar componentes ou ViewModels de Presentation umas das outras.
- Fullscreen é um componente de apresentação e usa callbacks do fluxo de Captures; não há ViewModel independente enquanto não existir estado de negócio próprio. O diálogo fullscreen também expõe a sub-checklist vinculada ao `captureId`.
- Checklists continua sendo compatibilidade de persistência e não deve ser reativado como experiência autônoma.
- `SharedContentIntentParser` em `features/imageimport/data/android` adapta Sharesheet/Intent para payloads simples; regras de criação e classificação de Capture ficam no `CaptureFactory` puro.
- Room permanece na versão 8; não criar migration nesta refatoração.

Antes de declarar a migração validada, exigir CI verde para o HEAD atual (`testDebugUnitTest` e `assembleDebug`) e revisar fluxos de compartilhamento, importação de imagem, OCR, categorias, conclusão/restauração, anotações e fullscreen.


## Feature Notes — editor de anotações

- A feature `notes` é responsável pela apresentação e edição das anotações de uma referência.
- O dado continua pertencendo ao agregado `Capture` e é persistido pela feature `captures`; Notes não acessa Room nem mantém uma cópia da fonte de verdade.
- `app/navigation` integra o callback do fullscreen com o editor Notes. Features não importam implementações internas de outras features.
- O editor deve oferecer área de texto ampla, contador de até 10.000 caracteres e ações explícitas de salvar/cancelar.
