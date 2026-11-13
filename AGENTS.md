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
- **Core:** infraestrutura e componentes realmente compartilhados entre features, incluindo utilitários de identificação de URLs YouTube usados tanto pelo Image Import quanto pelo Fullscreen. Não deve virar depósito genérico de regras de negócio ou serviços exclusivos de uma feature.
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
│   ├── util/                         # utilitários compartilhados (ex.: parser de URL YouTube)
│   ├── storage/                      # PrivateImageStorage + implementação
│   └── ui/                           # carregamento limitado de bitmaps
└── features/
    ├── captures/                     # Domain, Room Data, UseCases, ViewModel e biblioteca
    ├── imageimport/                  # gateway, parser Android, OCR, metadata e ViewModel
    ├── categories/                   # ViewModel e componentes de categorias
    ├── search/                       # filtro, estado e campo de pesquisa
    ├── fullscreen/                   # diálogo e página fullscreen
    ├── youtubeplayer/                 # player incorporado YouTube e integração WebView
    ├── dashboard/                    # resumo, UseCase e ViewModel
    ├── imagecompression/             # perfis e processamento local de imagens
    ├── notes/                         # blocos de pensamento com texto/imagem
    ├── newsletters/                  # feeds RSS/Atom, artigos e leitor
    ├── settings/                     # preferências e páginas informativas
    ├── backup/                       # exportação ZIP e restauração aditiva
    └── share/                        # compartilhamento nativo e exportação PDF/RTF
```

### 2.3 Responsabilidade de cada feature

- **captures:** ciclo de vida das referências: criar, observar, consultar por identificador, editar, concluir/restaurar e excluir. É a proprietária do modelo e das regras de referência.
- **imageimport:** entrada de links/textos/imagens quando aplicável ao fluxo de importação, Photo Picker, compartilhamento, screenshots, OCR e estado de processamento. Links do YouTube e dos demais sites persistem a thumbnail como capa quando o metadado e o download estão disponíveis; o player incorporado continua sendo responsabilidade do Fullscreen. Entrega dados validados/enriquecidos para persistência por Captures; não duplica a lógica de persistência de referências.
- **search:** consulta, busca textual, combinação de filtros e apresentação dos resultados. Consome contratos de consulta de Captures; não acessa seu DAO ou banco diretamente.
- **categories:** regras e operações de categorias, incluindo validações, ordenação e reatribuição segura de referências quando uma categoria é removida.
- **fullscreen:** experiência Details Fullscreen, pager, sincronização da referência ativa, título, anotações e ações disponíveis no detalhe. Identifica o vídeo da referência ativa e hospeda o player fornecido pela feature youtubeplayer; não implementa nem configura WebView.
- **captures:** é proprietária das referências e das subtarefas persistidas em `capture_subtasks`, vinculadas por `captureId` e apresentadas no fullscreen. A entidade/DAO histórica `checklist_tasks` permanece apenas para compatibilidade do schema Room, sem feature ou tela ativa.
- **dashboard:** indicadores e estatísticas de referências/categorias. Somente consulta; não modifica Captures diretamente.
- **imagecompression:** pipeline local adaptativo antes do armazenamento privado. Gera WebP e JPEG para imagens opacas e mantém o menor arquivo; preserva PNG/transparência com WebP lossless (PNG lossless como fallback). Perfis controlam qualidade dos encoders; orientação EXIF e limite de dimensão são aplicados. O contrato aceita URI (`file://`/`content://`) ou bytes raster codificados.
- **notes:** edição da linha de pensamento em blocos ordenados de texto e imagem. O modelo CaptureNote e CaptureNotesCodec pertencem ao domínio de Captures; o campo captures.notes armazena a coleção em formato versionado e mantém leitura retrocompatível de anotações antigas.
- **youtubeplayer:** reprodução incorporada de vídeos do YouTube via IFrame Player API em Android WebView. É proprietária do HTML de embed, identidade HTTP Referer, configuração WebView, ciclo de vida, callbacks de estado/erro e limpeza da instância. Não persiste estado de negócio nem depende de Captures/Fullscreen.
- **settings:** tema, ordenação dos containers de categoria e das referências na categoria expandida, perfil de compressão e páginas Privacidade/Tutoriais; preferências em SharedPreferences.
- **newsletters:** fontes RSS 2.0/Atom, atualização manual, persistência e leitura.
- **backup:** exportação/importação ZIP versionada de referências, categorias, subtarefas, mídias, anotações e Newsletters. v2 inclui fontes/artigos e importa v1/v2; restauração é aditiva, preserva dados locais e usa contratos públicos de Captures/Newsletters.
- **share:** compõe payload imutável de uma referência e suas imagens/anotações; oferece copiar texto, Android Sharesheet com imagens privadas via FileProvider e exportação PDF/RTF. Não altera a referência nem persiste cópias no banco.

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

- `MainActivity` permanece como entrada Android para launcher, Sharesheet/Intents, permissões de plataforma e permissões de plataforma e ciclo de vida do serviço de screenshot. Deve encaminhar payloads para o ponto de entrada apropriado, sem concentrar regras de negócio.
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
- Na biblioteca, o filtro de categorias exibe todas as categorias em duas linhas dentro de um único container com rolagem horizontal compartilhada (as duas linhas avançam juntas). O botão **Ver todas as categorias** permanece como atalho para o modal de seleção, sem ocultar categorias da view principal. Em `Todas`, renderizar um cartão de categoria somente se houver ao menos uma referência pendente; com uma categoria específica selecionada, manter o cartão mesmo sem pendentes e explicar o estado vazio da prévia.
- Referências concluídas exibidas em thumbnails e no fullscreen devem usar o componente reutilizável `CompletedBadge` da feature Captures, sobreposto à imagem no fullscreen. Não duplicar estilos/selo localmente em cada card.
- A sub-checklist pertence a uma referência e é persistida em `capture_subtasks`, associada por `captureId`. Não restaurar uma tela independente de tarefas sem aprovação.
- Uma screenshot detectada pelo serviço só deve ser persistida como referência após prévia, escolha de categoria e confirmação explícita do usuário; preservar a imagem original na galeria e não usar `ChecklistTaskEntity`.
- Preservar o comportamento de filtros, busca, categorias, progresso e conclusão documentado em `docs/current-state.md`; em caso de divergência, verificar código e registrar a inconsistência antes de alterar.
- A ordenação de containers de categoria oferece manual, mais recentes, mais antigas, nome A–Z/Z–A; datas derivam de createdAt das referências da categoria. Não altera thumbnails de preview.
- A ordenação das referências na categoria expandida é uma preferência separada: mais recentes, mais antigas, título A–Z/Z–A. Ambas são persistidas por Settings.
- Perfis adaptativos: Médio usa JPEG 70/WebP 78 e Alto JPEG 52/WebP 66; esses valores são parâmetros de qualidade, não percentuais de redução. Imagens opacas comparam WebP/JPEG e persistem o menor; PNG/alpha usa WebP lossless com PNG lossless como fallback. URIs `file://` usam `FileInputStream`; URIs de provedores usam `ContentResolver`, com fallback para descritor.
- Cartões compactos atualmente usam 272 dp conforme a diretriz visual registrada; não alterar dimensões sem validar o layout atual e a decisão de produto.

## 6. Captura, links, imagens e privacidade

- Entrada primária: Android Sharesheet / `ACTION_SEND` para texto, URLs e imagens, além do Photo Picker e detecção opt-in contínua de screenshots por Foreground Service.
- Preservar conteúdo recebido. Tratar origem/app/URL de origem como opcional, salvo quando verificada.
- Diferenciar conteúdo fornecido pelo usuário, resultado de OCR, metadados obtidos da web e sugestões inferidas.
- Evitar duplicatas para conteúdo textual normalizado e URLs conforme contrato atual. Não deduplicar imagens distintas usando texto placeholder genérico.
- OCR é local/on-device. Para referências de imagem, manter a imagem como capa e persistir o título extraído; não gravar OCR bruto em `content` sem decisão explícita.
- Copiar imagens compartilhadas para armazenamento privado do app quando possível. Se falhar, manter URI original como fallback e tratar falha de acesso sem perder a referência.
- Nunca excluir imagem escolhida manualmente pelo usuário.
- Exclusão da screenshot original só pode ocorrer no fluxo de importação confirmado pelo usuário dentro do app, após salvar referência e cópia privada. Falhas de exclusão são não fatais e podem exigir consentimento explícito do Android.
- Não fazer upload de conteúdo capturado sem consentimento explícito. Cloud sync está fora do escopo.
- Descoberta de screenshots é opcional e best-effort: `ScreenshotNotificationService` observa continuamente o MediaStore enquanto o usuário mantiver o recurso ativado, consultando metadados/nome/caminho e nunca pixels de outros apps. O ContentObserver pertence ao serviço e é encerrado em onDestroy. Nunca usar Accessibility Services ou captura de tela entre aplicativos para inferir screenshots.
- O fluxo de screenshot usa Foreground Service `specialUse` iniciado explicitamente pelo usuário, com notificação persistente e ação Desativar. Cada captura gera alerta individual; abrir o alerta apresenta prévia in-app e exige categoria e confirmação explícita antes de importar. A imagem original permanece na galeria.
- Antes de solicitar READ_MEDIA_IMAGES e POST_NOTIFICATIONS, exibir disclosure destacado explicando o monitoramento contínuo de metadados, a notificação persistente, a necessidade de acesso completo para detectar novas screenshots, a ausência de upload e a confirmação de importação. No Android 14+, declarar READ_MEDIA_VISUAL_USER_SELECTED e não iniciar o serviço com acesso parcial. Oferecer cancelamento sem solicitar permissões; manter Photo Picker disponível.
- READ_MEDIA_IMAGES permanece sujeito à política de acesso amplo a fotos do Google Play; Photo Picker deve continuar disponível para seleção manual. A detecção contínua não garante elegibilidade de READ_MEDIA_IMAGES; o Google Play pode exigir Photo Picker. O Foreground Service sem prazo de término também tem risco elevado diante do critério de executar apenas pelo tempo necessário para concluir a tarefa.

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
- Fotografar e selecionar imagens pelo Photo Picker aplicam o perfil escolhido (Médio/Alto) antes do OCR e do armazenamento privado. A compressão é local; se falhar, manter o fluxo de importação com a URI original sem apagar a origem.
- Imagens adicionadas aos blocos de anotação são recortadas antes da compressão; o recorte é codificado como PNG lossless em memória (`ByteArray`) e passa pelo mesmo encoder JPEG no perfil Alto, sem arquivo ou URI intermediária. A anotação só recebe a URI privada após sucesso; falhas mantêm a seleção e a prévia para nova tentativa. Nunca apagar imagem original selecionada pelo usuário.
- Não fazer requisição ilimitada nem baixar HTML/imagens sem limites.

## 7. Regras de fullscreen e navegação

- O visualizador fullscreen usa navegação horizontal com Compose `HorizontalPager`: swipe para esquerda avança; swipe para direita retorna. Respeitar a lista fornecida/filtrada e não fazer loop nas extremidades.
- O cabeçalho e as ações devem acompanhar a página efetivamente assentada (settled page), não apenas o índice solicitado.
- Manter proporção aproximada de 20% para cabeçalho e 80% para área de imagem.
- Imagem deve preservar `ContentScale.Fit`; a dica `‹ Deslize para navegar ›` é overlay na área da imagem e não pode reservar altura nem alterar a escala.
- Títulos longos podem sobrepor a imagem com gradiente escuro forte (quase opaco atrás do texto, dissolvendo para transparência), sem redimensionar a área reservada ao pager.
- O acesso à linha de pensamento fica no canto inferior esquerdo do fullscreen, com ícone de balão de conversa; abre diretamente o formulário de nova anotação da feature `notes`. Cada bloco aceita texto de até 10.000 caracteres e imagem opcional; toque prolongado em bloco existente abre edição/remoção. Seleção/troca de imagem deve apresentar recorte antes da compressão Alta. Cancelar não persiste.
- A coluna de ações fullscreen permanece no canto inferior direito. Para referências pendentes, manter checkbox, sub-checklist, Visualizar conteúdo quando elegível, editar e excluir; para concluídas, somente reabrir. O atalho de anotações no canto inferior esquerdo permanece independente dessa coluna.
- O modal **Editar título** no fullscreen permite editar o título e selecionar outra categoria no mesmo formulário. Persistir cada campo pela operação correspondente de Captures, sem alterar conteúdo, URL ou imagem.
- Quando `sourceUrl` for HTTP(S) válida, **Abrir link** fica no cabeçalho; **Visualizar conteúdo** (globo) fica no dock quando elegível. Não duplicar o atalho externo.
- Links YouTube válidos (`watch?v=`, `youtu.be`, Shorts, live e embed) são reconhecidos por `core.util.YoutubeVideoIdParser` e devem carregar diretamente o endpoint oficial `youtube-nocookie.com/embed/{id}` no WebView do sistema, com controles visíveis, sem autoplay e viewport mínimo de 200×200 dp. Como WebView não envia Referer por padrão, definir o cabeçalho `Referer` como `https://{applicationId}/` (identidade do app Android) em todo carregamento/recarregamento. Manter hardware acceleration explicitamente ativa no app, Activity e WebView para a superfície de vídeo; não aplicar camada de software ao WebView ou ancestrais. Permitir recursos internos do player e bloquear navegação principal para hosts externos. Image Import pode obter e persistir thumbnail/capa quando metadados/download estão disponíveis; o player não baixa vídeos. Manter link original como fallback.
- Na biblioteca inicial, `categorySortOrder` ordena os containers de categoria (`CategoryCard`); não aplicar essa preferência à ordem dos thumbnails de captura dentro do preview. A preferência `captureSortOrder` controla a ordenação das referências dentro da categoria expandida. Anotações pertencem à referência como uma lista ordenada de blocos; cada bloco aceita até 10.000 caracteres e uma imagem opcional. A lista aparece no canto inferior esquerdo em painel com altura natural do conteúdo, limitada a 248 dp e com rolagem vertical somente quando exceder esse limite. Toque prolongado em um bloco abre sua edição/remoção. O botão de balão abre diretamente o formulário de nova anotação. Imagens selecionadas devem permitir recorte antes de usar compressão Alta; em caso de falha, manter seleção/recorte para nova tentativa e exibir a causa retornada pelo serviço.
- O armazenamento usa o campo Room captures.notes existente, com serialização versionada. Texto legado é lido como um bloco para preservar dados; não adicionar migration destrutiva.
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
- Executar também verificações em dispositivo para Sharesheet, Photo Picker, permissões, ciclo de vida do Foreground Service e monitoramento contínuo, Back/gestos, pager e exclusão de arquivos privados.
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


## 10. Rebrand — Personal Notebook (aprovado em 2026-11-04)

A especificação visual canônica é [docs/design-system.md](docs/design-system.md). Ela define a direção Hybrid Personal Notebook, temas claro/escuro, tokens, tipografia, componentes, diretrizes por tela e contrato de não regressão.

**Regra obrigatória: o rebrand é presentation-only.** Não remover, adicionar ou alterar funcionalidades, ações, estados de negócio, navegação, gestos, callbacks, contratos, UseCases, ViewModels, schema Room, migrations, preferências persistidas, formatos serializados, intents, permissões, OCR, compressão, storage, player YouTube, Newsletters ou fluxos de captura.

Antes de alterar uma feature, inventariar os seus estados, ações, entradas/saídas, navegação, gestos, integrações e persistência; comparar com a documentação e o código atual. Se houver divergência documental, investigar e registrar antes de decidir. Preservar o comportamento observado no código e contratos funcionais mais recentes; nunca tratar wireframe ou plano visual como autorização para mudar comportamento.

A ordem de execução é: baseline e inventário; tokens/temas; componentes; Biblioteca; Fullscreen/Notes; Categorias/Dashboard/Newsletters/Settings; fluxos de captura; regressão visual e funcional. Executar testes relevantes, `testDebugUnitTest` e `assembleDebug` antes de concluir. Não combinar redesign com refatoração arquitetural ou mudança de produto.


## Melhorias funcionais aprovadas — tarefas manuais, capas e movimento (2026-11-04)

Estas melhorias são uma exceção explicitamente aprovada ao escopo presentation-only do rebrand:
- A Biblioteca oferece três entradas: Fotografar, Galeria e Nova tarefa. A tarefa manual exige título e categoria válida, é criada sem imagem, sem URL de origem e sem OCR. A criação pertence a Captures; a UI encaminha o evento pelo CaptureViewModel.
- Referências sem imagem usam `core/ui/components/TaskPaperCover.kt` como capa visual. A capa é desenhada em Compose e não gera arquivo, URI ou dado persistido.
- Na grade Concluídas, cada card expõe exclusão com confirmação. A ação reutiliza `CaptureViewModel.deleteCapture`, incluindo limpeza de subtarefas, mídias e imagens privadas.
- Animações são curtas e orientadas a feedback: fade/scale de cards, crossfade de capas, transição de estado e progresso. Evitar animações infinitas, loops, bitmaps decorativos e trabalho na main thread. Testar com escala de animação do sistema reduzida/desativada.
- Não adicionar dependências, permissões, migrations ou mudanças no schema Room para estas melhorias.


## Regras de navegação web interna (04/11/2026)

- Toda WebView nova deve manter o padrão seguro usado por Newsletters e Fullscreen: JavaScript e DOM Storage desativados por padrão, acesso a arquivos/conteúdo bloqueado, mixed content bloqueado e Safe Browsing habilitado.
- Validar URL antes de carregar: aceitar somente HTTP/HTTPS, exigir host e rejeitar user-info/credenciais. Redirecionamentos/navegações devem continuar limitados a HTTP/HTTPS; nunca permitir schemes como file, content ou javascript.
- Preferir Custom Tabs/navegador externo para fluxos que exijam JavaScript, login, downloads ou APIs nativas. Não habilitar JavaScript globalmente para fazer sites funcionarem.
- A WebView deve respeitar o histórico do botão Back e liberar recursos com `stopLoading()` e `destroy()` ao sair da tela.
- Manter a ação de navegador externo disponível como fallback. Não interceptar URLs de plataformas que já possuam fluxo dedicado (ex.: player YouTube).


## Conteúdo de privacidade e ajuda (2026-11-04)

- As telas Privacidade e Tutoriais são conteúdo informativo da feature Settings, acessadas pelo menu do cabeçalho.
- A política deve refletir o comportamento implementado: dados de referências, notas, categorias, subtarefas, preferências e imagens são locais; não há conta nem sincronização própria em nuvem.
- Não afirmar que o app é totalmente offline: importação de links, feeds RSS/Atom, leitor web e navegador externo podem acessar serviços de terceiros. Explicar esse compartilhamento técnico e não prometer controle sobre políticas de terceiros.
- Backup é exportado como ZIP versionado com manifesto JSON e imagens. A importação é aditiva, não apaga dados locais, preserva categorias já existentes e remapeia IDs em colisão. O usuário escolhe o destino e é responsável por guardar a cópia em local seguro.
- Revisar a tela de privacidade sempre que forem adicionados analytics, telemetria, contas, sincronização, anúncios, pagamentos ou novos serviços externos.


## Feature Backup — formato e mesclagem (2026-11-04)

- O formato inicial é ZIP com `manifest.json` versionado e arquivos em `images/`; não persistir URIs do aparelho de origem como se fossem caminhos válidos no destino.
- Exportar referências, categorias, subtarefas, mídias e anotações, incluindo imagens das capas, galerias e blocos de anotação.
- Importação é aditiva e não destrutiva. Categoria com nome já existente mantém sua ordenação local.
- Referência duplicada é identificada por URL normalizada e identidade textual/organizacional (título, conteúdo, tipo, categoria, conclusão e notas); sem URL, usa a mesma identidade sem a URL. Duplicatas são ignoradas. Colisão de ID com conteúdo diferente gera novo ID e os filhos devem apontar para esse novo ID.
- IDs de subtarefas e mídias também devem ser regenerados em colisão. Nunca sobrescrever silenciosamente dados locais.
- Inserção dos registros deve ser atômica no Room. Validar manifesto, versão, caminhos ZIP, tamanhos e imagens antes de confirmar os registros.
- Versão inicial não inclui Newsletters nem preferências; manter esse limite documentado até ampliação explícita do escopo.


## Regras das novas capacidades — 2026-11-04

- Backup v2 deve continuar lendo v1. A importação é aditiva; Newsletters existentes são reconhecidas por feedUrl e artigos por índices naturais Room, sem sobrescrever estado local.
- Novas imagens devem passar pelo ImageCompressionService. O encoder compara WebP/JPEG em imagens opacas e escolhe o menor; transparência exige WebP lossless. Não reprocessar automaticamente arquivos antigos sem plano de migração/rollback.
- Troca da capa usa o contrato público replaceCoverImage, atualiza a mídia marcada como capa e preserva a galeria restante. Só excluir o arquivo anterior depois de confirmar a gravação.
- Fullscreen pode expandir/recolher o painel Notes; isso é estado de UI e não altera o conteúdo persistido.
- Share recebe payload snapshot de Captures/Notes; exportadores PDF/RTF ficam na camada Data da feature Share. Compartilhamento de arquivo privado deve usar FileProvider com escopo restrito a files/captures e grant read-only.

## Auditoria documental — 2026-11-04

Base auditada: main em 155a49b34eb3e536ade189ce7e84c1344bb44560 (após PR #60). Precedência: código atual define comportamento; este arquivo define regras globais; docs/current-state.md descreve fluxo integrado; docs/features/{feature}/README.md descreve ownership. Handoffs e wireframes antigos são históricos até reconciliação.

Correções consolidadas: Settings mantém duas ordenações independentes; Fullscreen separa Abrir link no header de Visualizar conteúdo no dock; Image Import pode salvar thumbnail de YouTube; Newsletters e Backup são ativos; Backup v1 não incluía Newsletters; Backup v2 inclui fontes e artigos, enquanto preferências continuam fora; Checklists é legado e subtarefas pertencem a Captures. A tela Privacidade ainda contém texto legado dizendo que backup é futuro; corrigir UI em alteração de código separada.

Ao mudar uma feature, atualizar seu README em docs/features, o estado integrado se houver mudança de fluxo e o índice docs/features/README.md. Marcar documentos históricos e evitar regras contraditórias.


## Share — formato nativo e padrão de PDF (2026-11-05)

- O botão Compartilhar da feature Share envia um único documento PDF pela Android Sharesheet (ACTION_SEND, MIME application/pdf); não anexar imagens separadas (ACTION_SEND_MULTIPLE / image/*).
- O PDF é gerado antes de abrir a Sharesheet, salvo em cacheDir/share_exports/ e compartilhado exclusivamente por FileProvider com URI content:// e grant temporário read-only. Nunca expor file:// nem usar MIME genérico.
- O documento contém título, categoria, data, URL/conteúdo disponíveis, capa, galeria na ordem do payload e Notes ordenadas (texto seguido da imagem opcional). Não omitir silenciosamente imagens esperadas: falhar com mensagem compreensível.
- PDF nativo e PDF exportado pelo SAF usam o mesmo CaptureShareExporter e padrão visual definido em docs/features/share/README.md (A4 retrato, margens 48 pt, paleta Personal Notebook, tipografia hierárquica, imagens proporcionais, paginação e rodapé).
- Atualizar a documentação e os testes quando esse padrão for alterado. RTF e copiar texto permanecem opções separadas.


## Preparação para publicação — Google Play (2026-11-05)

- O produto oficial para distribuição é o app Android nativo em `android-native/`; não publicar o projeto histórico `legacy-expo/`.
- Manter `applicationId` estável como `com.fehunter.captureflow`. Não alterar o identificador após a primeira publicação.
- `compileSdk` e `targetSdk` devem acompanhar os requisitos vigentes do Google Play; qualquer alteração deve ser validada em dispositivo/emulador e documentada.
- `minSdk = 26` permanece até decisão explícita baseada em telemetria/compatibilidade.
- Nunca versionar keystore, senhas, certificados privados ou arquivos de configuração contendo segredos. Assinatura release recebe os valores por variáveis de ambiente/segredos do CI.
- A configuração release pode gerar bundle não assinado em CI para validar compilação. Um AAB só está apto para upload quando assinado com a chave de upload configurada e validado no Play Console.
- Não incrementar `versionCode` nem definir a versão pública 1.0.0 durante a preparação. Fazer isso somente na etapa de release aprovada, garantindo valor maior que qualquer versão já enviada ao Play Console.
- Antes da publicação, revisar cada permissão do Manifest contra o comportamento real, com atenção a `READ_MEDIA_IMAGES` e Photo Picker. A detecção de screenshots usa Foreground Service `specialUse`, iniciado por ação explícita, com notificação persistente e ação de parada. A elegibilidade no Google Play deve ser reavaliada na submissão; não afirmar aprovação antecipada.
- Revisar Android Auto Backup/transferência de dispositivo considerando dados locais, imagens privadas e o backup ZIP manual. Não desabilitar ou ampliar backup sem documentar o impacto para o usuário.
- Política de privacidade e Data Safety devem descrever o comportamento real, inclusive requisições externas de metadados, RSS/Atom, YouTube, navegador e compartilhamento escolhido pelo usuário. Não afirmar que o app é totalmente offline.
- O pipeline de CI deve executar testes unitários, assembleDebug, compilação dos testes instrumentados e bundleRelease. O bundleRelease sem credenciais é validação de build, não evidência de assinatura pronta para loja.
- Manter a preparação de publicação documentada em `docs/release/google-play-preparation.md` e atualizar este arquivo quando requisitos de release forem alterados.


## Política de backup e privacidade para publicação — 2026-11-05

- Android Auto Backup e transferência automática de dados devem permanecer desabilitados: Manifest com `android:allowBackup="false"`, `android:fullBackupContent="@xml/backup_rules"` e `android:dataExtractionRules="@xml/data_extraction_rules"`; regras devem excluir todos os domínios de backup em nuvem e device transfer.
- A feature ZIP manual é a única forma suportada de backup/restauração. Não prometer sincronização ou migração automática. Explicar que o usuário deve exportar no aparelho de origem, guardar o arquivo em local seguro e importar no aparelho de destino.
- A detecção opt-in de screenshots é contínua enquanto habilitada e usa `ScreenshotNotificationService` como Foreground Service `specialUse`. O serviço observa somente metadados do MediaStore, mantém notificação persistente com ação Desativar e usa alertas individuais que abrem prévia; a importação exige confirmação. `START_STICKY` permite tentativa de retomada após encerramento do processo, respeitando preferência e permissões; não há receiver de boot. O fluxo não captura tela, não usa Accessibility Services e não envia imagens para servidores.
- READ_MEDIA_IMAGES é utilizado para identificar screenshots novas enquanto o serviço está ativo; READ_MEDIA_VISUAL_USER_SELECTED identifica acesso limitado no Android 14+ e não habilita o serviço. POST_NOTIFICATIONS é necessário para os avisos no Android 13+; FOREGROUND_SERVICE e FOREGROUND_SERVICE_SPECIAL_USE sustentam o serviço declarado no Manifest. Photo Picker permanece como alternativa manual. A justificativa Play Console deve explicar a finalidade, ativação explícita, notificação persistente, parada pelo usuário e impacto de interrupção, sem afirmar que isso garante elegibilidade.
- A tela Privacidade deve refletir o comportamento atual: dados locais, requisições externas iniciadas por recursos de links/feeds/web, permissões de câmera/imagens, compartilhamento de PDF para apps escolhidos pelo usuário e backup exclusivamente manual em ZIP. Não declarar o app totalmente offline.


## Android release optimization and edge-to-edge

- Release builds must run R8 with code shrinking, optimization and obfuscation enabled (\`isMinifyEnabled = true\`) and resource shrinking enabled (\`isShrinkResources = true\`).
- Keep the standard optimized Android ProGuard configuration. Add custom keep rules only for code genuinely accessed through reflection/JNI/serialization and only at the narrowest possible scope; do not add blanket package-wide keeps or disable obfuscation/optimization to silence warnings.
- Validate every release AAB through \`:app:bundleRelease\`; CI must confirm the R8 mapping output exists and retain non-sensitive R8 diagnostics. A successful build does not prove the Play Console's measured percentages: inspect DEX optimization, shrinking and obfuscation metrics for the uploaded bundle in Play Console.
- Preserve the mapping file securely for each published release so crash traces can be retraced. Do not commit it or expose it in public artifacts.
- \`MainActivity\` must explicitly call \`enableEdgeToEdge()\` for backward-compatible edge-to-edge behavior. Compose Material 3 scaffolds/app bars should own their standard system insets; avoid applying duplicate status/navigation bar padding.
- System bar icon contrast must follow the app's selected light/dark theme, including when it differs from the device theme. Validate status/navigation bars, display cutouts, gesture and 3-button navigation, keyboard/IME, dialogs and fullscreen surfaces on Android 15+ (API 35/36) and at least one pre-35 API.
