# Capture Flow — UI/UX Design Handoff

> **Objetivo:** entregar ao Design uma descrição funcional e visual do aplicativo Android atualmente implementado, para criação/renovação da identidade visual sem quebrar fluxos, estados ou contratos de produto.
>
> **Data da análise:** 04/11/2026  
> **Base analisada:** branch `main` do repositório `FeHunter/captureflow`.  
> **Plataforma ativa:** Android nativo — Kotlin, Jetpack Compose e Material 3.  
> **Escopo:** telas, diálogos, superfícies, componentes e comportamentos visíveis no código ativo. `legacy-expo/` é histórico e não faz parte do escopo de redesign.

---

## 1. Como usar este documento

Este documento descreve o **comportamento atual**, não uma proposta de redesign. O Design pode reorganizar hierarquia, composição, tipografia, cores, ícones, espaçamento, elevação, superfícies e microinterações, desde que preserve os comportamentos e regras identificados como contrato.

Classificação utilizada:

- **Implementado:** comportamento encontrado no código Android ativo.
- **Regra de produto:** comportamento que deve ser preservado conforme `AGENTS.md`.
- **Atenção / validar:** divergência documental, nomenclatura antiga ou comportamento que merece confirmação em dispositivo antes de fechar os layouts.

Não interpretar recursos citados em documentação histórica ou na pasta `legacy-expo/` como telas ativas.

## 2. Visão do produto e princípios de experiência

Capture Flow é uma biblioteca pessoal local-first para guardar referências provenientes de links, textos, imagens, fotografias e screenshots. O conteúdo é organizado por categorias e pode receber anotações e subtarefas.

A experiência atual prioriza:

1. **Capturar rapidamente:** receber conteúdo do Android Sharesheet, escolher uma imagem na galeria ou fotografar dentro do app.
2. **Revisar antes de persistir:** importações de imagem, link e screenshot têm confirmação explícita, conforme o fluxo de origem.
3. **Organizar por categoria:** visualizar grupos de referências e acessar rapidamente uma categoria.
4. **Acompanhar progresso:** distinguir referências pendentes e concluídas sem mover o item para outra categoria.
5. **Consultar o conteúdo:** abrir uma referência em tela cheia, navegar entre itens e acessar seu link original quando existir.
6. **Desenvolver uma ideia:** registrar anotações em blocos de texto/imagem e subtarefas associadas à referência.

### Contratos que o redesign não deve alterar

- Existe **uma única tela principal de Referências**. Não existe bottom navigation nem uma tela independente de tarefas.
- Categorias padrão: **Ler depois**, **Verificar**, **Salvar** e **Importante**. **Todas** é um filtro, não uma categoria persistida.
- Conclusão é um estado independente (`isCompleted`): concluir/reabrir não muda a categoria.
- Sub-checklist pertence à referência atual e é persistida por `captureId`.
- Dados e imagens permanecem no dispositivo. Não há sincronização em nuvem nem upload de conteúdo capturado.
- OCR é executado localmente no dispositivo. O título sugerido pode ser revisado; falha de OCR não deve eliminar a imagem/referência.
- O usuário deve escolher uma categoria válida antes de confirmar a criação nos fluxos de link, imagem pendente e screenshot via notificação.
- Não apresentar `Concluído` como categoria ou opção de categoria.
- Não reintroduzir a antiga experiência de tarefas/checklists independentes.

## 3. Estrutura de navegação atual

A navegação é deliberadamente rasa. O usuário parte da Biblioteca e abre superfícies temporárias sobre ela.

```text
Capture Flow — Biblioteca (tela raiz)
├── Menu superior
│   ├── Mostrar/Ocultar concluídos
│   └── Tema: dispositivo / claro / escuro
├── Adicionar imagem
│   ├── Câmera
│   │   └── Ajustar foto / recorte
│   │       └── Revisar nova captura
│   └── Galeria (Photo Picker)
│       └── Revisar nova captura
├── Busca
├── Meu progresso
├── Categorias
│   ├── Ordenar categorias
│   ├── Gerenciar categorias
│   ├── Filtrar por categoria
│   └── Categoria expandida
│       └── Grade de referências
│           └── Detalhes fullscreen
│               ├── Editar título/categoria
│               ├── Sub-checklist
│               └── Anotações
│                   ├── Nova anotação
│                   ├── Editar/excluir anotação
│                   └── Visualizar imagem ampliada
├── Confirmação de link compartilhado
├── Confirmação de screenshot
└── Confirmação de imagem detectada/selecionada
```

**Back do Android:** fecha primeiro a superfície/modal superior. No fullscreen, Back fecha o visualizador e mantém a categoria expandida por baixo; outro Back retorna à Biblioteca.

## 4. Inventário de telas e superfícies

### 4.1 Biblioteca — tela principal

**Ponto de entrada:** `CaptureFlowNavigation.kt` → `CaptureLibraryCoordinator.kt`.

É a tela raiz e a superfície mais importante do produto. Conteúdo vertical rolável, com os blocos nesta ordem:

1. Top bar.
2. Cartão **Adicionar imagem**.
3. Campo de pesquisa.
4. Dashboard **Meu progresso**.
5. Cabeçalho **Categorias**, ações de ordenar e gerenciar.
6. Filtros de categoria.
7. Cartões de categoria com prévias das referências ou estado vazio.

**Top bar**
- Nome **Capture Flow** à esquerda.
- Ação de monitoramento de screenshots (ícone de notificação ligado/desligado).
- Menu de opções (três pontos).
- Menu de opções: mostrar/ocultar referências concluídas e abrir seleção de tema.
- Tema: seguir dispositivo, claro ou escuro. A preferência é persistida localmente.

**Adicionar imagem**
- Cartão de destaque com ícone, título e texto explicando a extração local de texto.
- Ações: **Fotografar** e **Galeria**.
- A câmera é uma tela de captura do próprio app; Galeria abre o Android Photo Picker.
- A seleção da galeria inicia preparação/revisão de imagem; não cria referência imediatamente.

**Busca**
- Campo de largura total, ícone de busca e placeholder **Pesquisar…**.
- Botão de limpar aparece quando existe texto.
- Busca combina texto do título/conteúdo com a categoria selecionada.
- Estado sem resultados: **Nenhuma referência encontrada.**
- Estado sem referências cadastradas: orientação para selecionar uma imagem ou compartilhar conteúdo de outro app.

**Categorias e ordenação**
- Cabeçalho com ação **Ordenar** e ação **Gerenciar**.
- Menu Ordenar oferece apenas a ordenação dos containers de categoria: ordem padrão, mais recentes, mais antigas, nome A–Z e Z–A.
- A ordenação por data considera o timestamp das referências dentro de cada categoria. Não muda a ordem dos thumbnails de prévia.
- Filtros de categoria aparecem em duas linhas dentro de um container com rolagem horizontal compartilhada.
- Todas as categorias ficam acessíveis na própria faixa; **Ver todas as categorias** abre um diálogo de seleção da lista completa.
- O filtro **Todas** mostra cartões de categoria que tenham referências pendentes. Ao selecionar uma categoria específica, seu cartão pode continuar visível mesmo sem pendentes, com mensagem de estado vazio.

**Comportamentos globais**
- A opção Mostrar/Ocultar concluídos controla a inclusão de referências concluídas na biblioteca.
- A busca e o filtro de categoria atuam sobre o conteúdo exibido.
- O conteúdo principal permanece em uma única coluna vertical; não há navegação inferior.

### 4.2 Dashboard — Meu progresso

**Componente:** `DashboardSummaryCard`.

- Card com título **Meu progresso**.
- Destaque superior para total de referências concluídas, ícone de conclusão, número e mensagem contextual.
- Seção **Progresso por categoria**.
- Cada categoria mostra nome, quantidade concluída, quantidade pendente e barra horizontal proporcional.
- Exibe cinco categorias inicialmente.
- Quando houver mais, ação **Ver todas as categorias (N)** expande; **Recolher categorias** retorna ao estado compacto.
- Categorias sem referências não aparecem.
- Os totais são globais: não mudam com a busca, filtro de categoria ou opção de ocultar concluídos.

**Diretriz de redesign:** preservar leitura rápida dos números e a distinção entre total concluído e progresso por categoria. A barra é informativa, não um controle de interação.

### 4.3 Cartão de categoria — prévia na Biblioteca

**Componente:** `CategoryCard`.

- Container de largura total, identificado pelo nome da categoria.
- Exibe até três thumbnails de referências pendentes.
- Exibe progresso concluídas/total e ação **Ver todas · quantidade**.
- Em **Todas**, categorias sem referências pendentes não exibem cartão.
- Com categoria específica selecionada, o cartão permanece e apresenta mensagem quando não existem pendentes.
- Toque em thumbnail abre diretamente os detalhes fullscreen.
- Toque em **Ver todas** expande a categoria.

**Thumbnails**
- Imagem em proporção quadrada, com corte para preencher a área (`Crop`).
- Título sobreposto na parte inferior com gradiente escuro.
- Placeholder quando não existe imagem acessível.
- Selo `CompletedBadge` identifica referências concluídas quando exibidas.
- Indicador discreto de processamento/erro de OCR pode aparecer no canto superior.
- Não substituir a imagem original por um ícone genérico quando a imagem está carregando; prever estados de carregamento, indisponibilidade e ausência de imagem.

### 4.4 Categoria expandida — grade de referências

- Abre como superfície em tela cheia sobre a Biblioteca.
- Cabeçalho com botão de voltar/fechar, nome da categoria e contagem total/concluída.
- Dois filtros: **Pendentes · N** e **Concluídas · N**.
- Abre inicialmente em Pendentes.
- Grade de três colunas.
- Cada item é um thumbnail clicável que abre o fullscreen.
- Ordenação padrão dos itens é mais recentes primeiro; não há controle visível de ordenação de referências nesta tela.
- Estado vazio contextual: nenhuma referência pendente ou nenhuma concluída nesta categoria.
- Back fecha a categoria expandida e retorna à Biblioteca.

### 4.5 Detalhes da referência — fullscreen

**Componente:** `FullscreenCaptureDialog` + `FullscreenCapturePage`.

É uma superfície de tela cheia com fundo preto para dar prioridade à imagem/conteúdo.

**Cabeçalho**
- Data e hora local de criação.
- Categoria em chip.
- Ação para abrir URL original, quando a referência possui URL HTTP(S) válida.
- Ação para fechar.
- A edição do título/categoria é acessada pela coluna de ações, não por uma ação primária no cabeçalho.

**Área de conteúdo**
- Imagem em `ContentScale.Fit`, sem distorção ou corte.
- Fundo preto; a imagem usa o espaço restante.
- Título sobre a imagem com gradiente escuro forte que se dissolve até transparente.
- Indicador sobreposto **‹ Deslize para navegar ›** na região inferior; não reserva espaço no layout.
- Swipe para esquerda avança e swipe para direita retorna dentro da lista de referências da mesma categoria/contexto filtrado.
- Não existe loop nas extremidades.
- Data, categoria, estado e conteúdo acompanham a página estabilizada.
- Para links de YouTube reconhecidos, exibe player incorporado em proporção 16:9. O player não inicia automaticamente. Se o vídeo não permitir incorporação ou falhar, a URL original continua disponível.

**Coluna de ações flutuante — canto inferior direito**
- Superfície vertical com fundo de alta opacidade, cantos arredondados e elevação.
- Referência pendente:
  - Checkbox para concluir.
  - Acesso à sub-checklist com contagem concluídas/total.
  - Abrir link original (quando existir).
  - Editar título/categoria.
  - Excluir referência.
- Referência concluída:
  - Exibe somente o checkbox para reabrir.
  - As demais ações da coluna não aparecem nesse estado.
- A exclusão fecha o visualizador e remove a referência; o app também trata subtarefas e arquivos privados associados.

**Anotações no fullscreen — canto inferior esquerdo**
- Botão separado da coluna direita para adicionar/editar anotações.
- Quando existem anotações, blocos de texto/imagem aparecem acima do botão em painel escuro translúcido.
- Painel acompanha a altura natural do conteúdo, limitado a 248 dp; rolagem vertical ocorre quando necessário.
- Texto não deve ser truncado artificialmente em duas linhas.
- Toque simples na imagem do bloco abre visualização ampliada; toque prolongado no bloco/imagem abre edição.
- Preservar contraste suficiente sobre imagens claras e escuras.

**Editar título/categoria**
- Diálogo acessado pelo ícone de edição.
- Permite alterar o título e escolher uma categoria existente.
- A alteração é persistida e atualiza o detalhe ativo.

**Sub-checklist — bottom sheet**
- Acessada pela ação de checklist no fullscreen quando a referência está pendente.
- Cabeçalho com título **Sub-checklist** e contagem concluídas/total.
- Lista de itens com checkbox, título e ação de excluir.
- Campo para adicionar item e ação de inclusão.
- Permite adicionar, concluir e excluir subtarefas.
- Itens permanecem associados à referência atual.
- Não é uma lista global nem uma tela independente de tarefas.

### 4.6 Editor de anotações — nova/editar

**Componente:** `NotesEditorDialog`.

- Diálogo amplo, rolável, com título **Nova anotação** ou **Editar anotação**.
- Exibe título da referência como contexto.
- Campo de texto amplo (até 10.000 caracteres), com contador.
- Pode anexar uma imagem, trocar ou remover imagem existente.
- Ao selecionar/trocar imagem, há etapa de recorte com formatos Original, 1:1, 4:5 e 16:9.
- A prévia da imagem pode ser ampliada.
- Ações explícitas: **Cancelar** e **Salvar anotação**.
- Em edição, pode existir ação **Excluir**.
- Salvar fica indisponível durante processamento da imagem ou quando não há conteúdo de texto/imagem válido.
- Cancelar descarta as alterações não salvas e arquivos temporários gerados pelo app.
- Falhas de leitura/recorte/compressão exibem mensagem de erro e devem permitir nova tentativa sem perder o rascunho.

### 4.7 Visualização ampliada da imagem de anotação

- Diálogo em tela cheia, fundo preto com alta opacidade.
- Imagem centralizada em `Fit`, preservando proporção.
- Toque fora ou botão Fechar encerra a visualização.
- Back do Android também deve fechar esta superfície antes de fechar o editor/detalhe.

### 4.8 Câmera — Nova captura

**Componente:** `ImageCaptureScreen`.

- Abre em tela cheia, com preview da câmera traseira.
- Solicita permissão da câmera quando o usuário inicia o fluxo.
- Cabeçalho com fechar e título **Nova captura**.
- Toque no preview inicia foco na região tocada.
- Texto auxiliar: **Toque na imagem para ajustar o foco**.
- Botão circular de disparo centralizado na parte inferior.
- Durante a captura, botão apresenta indicador de progresso e bloqueia novo disparo.
- Erros de inicialização/captura aparecem sobre o preview.
- Sem permissão: mensagem explicativa e ação **Permitir câmera**.
- Depois de fotografar, a pessoa segue para ajuste de foto antes da revisão; a imagem permanece temporária até confirmação.

### 4.9 Ajustar foto — recorte após câmera

**Componente:** `ImageCropReviewScreen`.

- Tela cheia, título **Ajustar foto** e instrução para escolher enquadramento.
- Prévia da imagem.
- Formatos: Original, 1:1, 4:5 e 16:9.
- A seleção muda a prévia do recorte central.
- Ações **Cancelar** e **Usar foto**.
- Durante exportação, apresenta progresso e bloqueia ações conflitantes.
- Erro de leitura ou salvamento deve ser comunicado sem simular sucesso.
- Após confirmar, segue para a revisão de captura (título/categoria/OCR).

### 4.10 Revisar nova captura — imagem de galeria/câmera/compartilhamento

**Superfície:** diálogo criado pelo `CaptureLibraryCoordinator` com estado de `ImageImportViewModel`.

Entradas convergentes:
- Foto capturada pela câmera (depois do recorte).
- Imagem escolhida pelo Photo Picker.
- Imagem recebida por compartilhamento do Android.

Elementos:
- Título **Revisar nova captura**.
- Prévia da imagem, sem corte forçado.
- Campo editável de título.
- Seleção obrigatória de categoria.
- Estado de processamento OCR com indicador e texto **Analisando imagem com OCR…**.
- Mensagem de erro/resultado quando aplicável.
- Ações **Salvar** e **Cancelar**.
- Salvar só fica habilitado quando OCR terminou, o título não está vazio e a categoria é válida.
- Cancelar descarta a captura pendente; não cria referência.
- OCR é sugestão: título pode ser editado manualmente.
- Falha de OCR não deve apagar a imagem; usuário pode informar título.

**Atenção de nomenclatura:** o código atual contém rótulos antigos como **Título da tarefa** e **Salvar tarefa** em alguns pontos. O produto atual chama o objeto de **referência**. O Design deve padronizar a linguagem para referência/captura, sem introduzir uma área de tarefas.

### 4.11 Compartilhamento de link — confirmação

**Entrada:** Android Sharesheet com URL HTTP(S).

- URL recebida entra como captura pendente; não é persistida antes da confirmação.
- Diálogo **Salvar link como referência**.
- Seleção obrigatória de categoria (nenhuma pré-selecionada).
- Campo editável **Título principal**.
- URL original apresentada em texto secundário, truncada quando longa.
- Indicador **Buscando informações do link…** durante consulta de metadados.
- Se a busca falhar, mensagem informa que não foi possível obter o título automaticamente; o link permanece salvável com título fallback.
- **Salvar referência** fica habilitado somente quando não há carregamento, a categoria foi selecionada e o título não está vazio.
- **Cancelar** descarta o link pendente sem persistir.
- Após confirmar, tenta baixar capa para armazenamento privado quando disponível.
- A URL original permanece associada à referência.

**Comportamento técnico relevante ao Design:** não é um navegador embutido nem uma página de edição completa. A consulta de metadados é best-effort; não depender de descrição/imagem para desenhar o estado válido.

### 4.12 Screenshot detectada — confirmação por notificação

Há duas entradas de screenshot no código e elas não devem ser confundidas:

**A. Screenshot aberta pela ação de importação da notificação**
- Diálogo **Adicionar screenshot às referências?**
- Texto explica que a captura foi aberta pela notificação.
- Prévia da imagem.
- Seleção obrigatória de categoria.
- Ação **Salvar referência** só é habilitada após selecionar categoria.
- **Cancelar** fecha sem criar referência.
- Após salvar, o app copia a imagem para armazenamento privado quando possível e pode tentar remover a screenshot original conforme o fluxo opt-in; falha na remoção não deve impedir a referência.

**B. Screenshot detectada/selecionada em outro caminho**
- Diálogo **Screenshot detectada** ou **Vincular imagem a uma tag**.
- Texto contextual conforme origem.
- Seleção de categoria.
- Ação **Enviar para o app** ou **Agora não**.
- O monitoramento é opcional, configurado pelo usuário e depende das permissões Android de imagens/notificações.
- A detecção é best-effort por metadados/nome/localização no MediaStore; não analisa pixels de outros apps.

**Controles de monitoramento**
- Ícone de notificação na top bar representa monitoramento ligado/desligado.
- Quando ativo, tocar permite desativar; quando inativo, inicia solicitação/configuração de permissões.
- O monitoramento exige explicação clara, consentimento e forma de desativar.

### 4.13 Gerenciar categorias

**Componente:** `CategoryManagementDialog`.

- Diálogo **Gerenciar categorias**.
- Texto de apoio explica que referências existentes acompanham as alterações.
- Campo **Nova categoria** e ação de adicionar.
- Lista de categorias existentes.
- Categorias padrão/fixas recebem indicação **Padrão** e não exibem ações de renomear/excluir.
- Categorias personalizadas podem ser renomeadas inline, com salvar e cancelar.
- Categorias personalizadas podem ser excluídas.
- Excluir abre confirmação separada, explicando que referências e registros históricos serão realocados para outra categoria.
- Ação final **Concluir** fecha o diálogo.
- A lista pode rolar quando exceder a altura disponível.

### 4.14 Filtrar por categoria — diálogo de todas

- Ação **Ver todas as categorias** abre diálogo **Filtrar por categoria**.
- Lista completa de categorias em chips de largura total, com rolagem vertical.
- Selecionar categoria aplica o filtro e fecha o diálogo.
- **Fechar** encerra sem mudar a seleção.

### 4.15 Tema e preferências visíveis

- Não existe uma tela independente de Configurações no fluxo atual.
- A preferência de tema está no menu superior: **Seguir dispositivo**, **Claro**, **Escuro**.
- A ordenação de categorias está no menu **Ordenar** da seção Categorias.
- Perfil de compressão é uma preferência interna consumida nos fluxos de imagem; não há, no shell visível analisado, uma tela dedicada para editar esse perfil.
- Preferências são locais/persistidas.

## 5. Sistema visual existente — ponto de partida, não restrição estética

O tema atual é definido diretamente em `MainActivity.kt`, com esquemas claro e escuro customizados. Não foi identificada uma biblioteca de design system visual separada.

**Paleta atualmente codificada:**

| Token / uso | Claro | Escuro |
|---|---|---|
| Primária | `#1F5F8B` | `#63C4F0` |
| Secundária | `#164B62` | `#58B9D6` |
| Terciária | `#39A6D6` | `#8ABFE8` |
| Fundo | `#F4F8FC` | `#07182B` |
| Superfície | `#FFFFFF` | `#0C2B46` |
| Variante de superfície | `#E1EDF6` | `#164B62` |
| Texto principal | `#07182B` | `#F3F7FB` |
| Texto secundário | `#3D5D73` | `#B7CEDF` |

O tema usa Material 3 e alternância claro/escuro. O redesign pode substituir a identidade azul/navy atual; deve, porém, entregar tokens semânticos completos para os dois temas e manter contraste, legibilidade e estados de feedback.

Não foi encontrada uma definição de tipografia de marca, escala tipográfica customizada ou biblioteca própria de ícones nos arquivos ativos analisados. A interface usa componentes Material 3 e ícones Material.

## 6. Componentes e padrões reutilizáveis para o Design

Criar especificações e variantes para:

- Top app bar com ações de estado e menu overflow.
- Botão primário, secundário/outlined, texto e icon button.
- Campo de busca com estado vazio/preenchido e ação limpar.
- Chips de filtro (selecionado/não selecionado/desabilitado).
- Chips de categoria e estados de seleção obrigatória.
- Cartão de ação de captura com ações lado a lado.
- Card de progresso e barra de progresso.
- Card de categoria com prévias de mídia.
- Thumbnail de referência com título sobreposto, placeholder e badge de conclusão.
- Badge de concluído reutilizável.
- Indicadores de OCR: em processamento, sucesso implícito e erro.
- Dialog padrão, dialog com conteúdo rolável e dialog de confirmação destrutiva.
- Bottom sheet de sub-checklist.
- Painel de anotação sobre imagem.
- Coluna flutuante de ações no fullscreen.
- Player de vídeo 16:9 com loading/erro.
- Estados vazios, permissões, carregamento e erro.
- Controles de seleção de proporção de imagem.
- Controles de captura de câmera e foco.

### Estados visuais mínimos a especificar

| Componente/fluxo | Estados necessários |
|---|---|
| Busca | Vazia, com consulta, limpar, sem resultados |
| Categoria | Normal, selecionada, sem pendentes, lista longa |
| Thumbnail | Com imagem, sem imagem, carregando, OCR processando, OCR com erro, concluída |
| Progresso | Zero, parcial, completo, muitas categorias |
| Importação de imagem | Carregando, título sugerido, sem OCR, erro, pronto, salvando, cancelado |
| Link compartilhado | Carregando metadados, metadados disponíveis, fallback, categoria não escolhida, pronto para salvar |
| Screenshot | Permissão necessária, monitoramento desligado/ligado, prévia, categoria não escolhida, pronto para salvar |
| Câmera | Solicitando permissão, negada, preview, foco, capturando, erro, revisão |
| Fullscreen | Referência pendente/concluída, com/sem link, imagem/vídeo, com/sem anotação, início/meio/fim do pager |
| Sub-checklist | Vazia, itens pendentes, parcial, completa |
| Anotação | Nova, edição, texto, imagem, recorte, salvando, erro |
| Tema | Claro, escuro e seguir dispositivo |

## 7. Diretrizes de experiência e acessibilidade

1. **Hierarquia:** a referência e sua mídia devem ser o foco; ações secundárias não devem competir com o conteúdo.
2. **Consistência:** uma mesma ação deve manter ícone, rótulo, posição e feedback consistentes entre superfícies.
3. **Confirmação:** salvar, cancelar, excluir e concluir precisam ter affordance e feedback inequívocos.
4. **Estados de espera:** OCR, metadados, carregamento de imagem, câmera e player precisam de estados claros; não usar loading infinito sem alternativa/erro.
5. **Erros recuperáveis:** manter rascunhos e permitir tentar novamente sempre que o contrato atual permitir.
6. **Toque:** dimensionar áreas interativas para uso confortável em dispositivos Android; não depender apenas de ícones pequenos ou gestos pouco descobertos.
7. **Contraste:** título sobre imagens, painel de anotações, badges e controles flutuantes precisam funcionar em fundos variados.
8. **Texto:** evitar truncamento de títulos e anotações quando o contexto pede leitura; usar expansão/rolagem quando necessário.
9. **Orientação e proporção:** nunca distorcer imagens; distinguir visualização Fit de thumbnails Crop.
10. **Feedback de conclusão:** comunicar o estado concluído sem sugerir mudança de categoria.
11. **Permissões:** explicar por que câmera, imagens e notificações são solicitadas, antes/de forma contextual; oferecer caminho de recuperação quando negadas.
12. **Tema:** entregar versões claro e escuro completas, inclusive overlays, diálogos, estados de erro e superfícies de mídia.

## 8. Pontos de atenção encontrados na auditoria

### 8.1 Nomenclatura inconsistente
Alguns textos da interface ainda usam “tarefa”, “task” ou “tag” para objetos que hoje são referências/categorias. Exemplos encontrados: “Título da tarefa”, “Salvar tarefa”, “Nenhuma task pendente” e “Vincular imagem a uma tag”. Recomenda-se normalizar para “referência”, “categoria” e “captura” conforme o contexto.

### 8.2 Documentação histórica divergente
Há documentos antigos em `docs/features/captures/feature.md` com categorias e layout que não correspondem às regras atuais. Para produto/arquitetura, prevalece `AGENTS.md`; para comportamento atual, conferir o código e `docs/current-state.md`, registrando divergências. Não reutilizar wireframes antigos sem revisão.

### 8.3 Versão do banco
O código atual de `CaptureDatabase.kt` declara Room schema version **9**. Trechos do `AGENTS.md` e de documentos de migração anteriores ainda citam versão 8. Isso não muda o trabalho visual, mas a documentação técnica deve ser reconciliada em uma atualização própria; não fazer alterações de banco durante o redesign.

### 8.4 Ordenação
A UI visível expõe ordenação de categorias. Não desenhar um menu de ordenação de referências na grade expandida: o contrato atual mantém a ordem padrão mais recentes primeiro.

### 8.5 Câmera e revisão
O fluxo da câmera possui etapa de recorte antes da revisão OCR; Photo Picker/compartilhamento convergem para revisão sem necessariamente passar pela mesma tela de recorte. Não unificar visualmente as etapas de forma que elimine a diferença de fluxo.

### 8.6 Duas entradas de screenshot
Existem dois caminhos de entrada/confirmacão para screenshot (ação de importação pela notificação e caminho de screenshot detectada/selecionada). O Design deve considerar os dois estados e a explicação de permissões.

### 8.7 Configurações
Não desenhar uma tela de Configurações como se já existisse. Tema e ordenação são controles contextuais; perfil de compressão é preferência interna sem tela dedicada identificada.

## 9. Entregáveis esperados do Design

1. **Identidade visual:** direção de marca, linguagem visual, cores e tipografia.
2. **Design tokens:** cores semânticas, tipografia, espaçamento, raios, elevação, bordas, ícones e motion.
3. **Tema claro e escuro:** cobertura completa, inclusive overlays e diálogos.
4. **Biblioteca principal:** composição e hierarquia dos blocos, com foco na leitura da biblioteca e das categorias.
5. **Componentes:** variantes e estados documentados para os componentes da seção 6.
6. **Fluxos críticos:** importação de link, imagem, câmera, screenshot, categoria expandida, fullscreen, sub-checklist e anotações.
7. **Responsividade:** layouts para telas Android estreitas e maiores, fontes ampliadas e conteúdo longo.
8. **Protótipo navegável:** interações de seleção, abrir/fechar, swipe, bottom sheet, dialogs e estados de carregamento/erro.
9. **Handoff para desenvolvimento:** especificações de componentes, tokens, dimensões, assets e comportamento de estados.

## 10. Critérios de aceite do handoff

- Todas as telas/superfícies da seção 4 estão representadas ou explicitamente marcadas como fora do redesign.
- Nenhuma tela de tarefas independente ou bottom navigation é adicionada.
- As quatro categorias padrão e o filtro Todas estão corretamente representados.
- Conclusão é independente da categoria.
- Biblioteca, categoria expandida e fullscreen têm estados pendentes/concluídos coerentes.
- Sub-checklist e anotações permanecem vinculadas à referência.
- Os fluxos de link, imagem e screenshot preservam confirmação explícita e seleção de categoria.
- A câmera mantém captura, foco e recorte; imagens de galeria/compartilhadas mantêm o fluxo de revisão.
- Player YouTube, imagem Fit, thumbnails Crop e anotações sobrepostas têm estados visuais.
- Temas claro/escuro cobrem todos os componentes.
- A linguagem da interface utiliza “referência” e “categoria” de forma consistente.
- Nenhum comportamento planejado é apresentado como já implementado.

## 11. Arquivos de referência no repositório

### Fonte de verdade e visão de produto
- `AGENTS.md` — regras arquiteturais e contratos de produto.
- `docs/current-state.md` — comportamento atualmente documentado; conferir divergências apontadas neste handoff.
- `docs/project-overview.md` — visão geral do produto.
- `docs/architecture.md` — arquitetura implementada.

### UI e features
- `docs/features/captures/uiux_current.md`
- `docs/features/captures/wireframes.md`
- `docs/features/captures/card-layout.md`
- `docs/features/captures/feature.md` (histórico; revisar divergências)
- `docs/features/image-capture/README.md`
- `docs/features/image-import/ocr-title-detection.md`

### Código Android ativo
- `android-native/app/src/main/java/com/fehunter/captureflow/MainActivity.kt`
- `app/navigation/CaptureFlowNavigation.kt`
- `app/navigation/CaptureLibraryCoordinator.kt`
- `features/captures/presentation/components/CaptureComponents.kt`
- `features/categories/presentation/components/`
- `features/dashboard/presentation/components/DashboardSummary.kt`
- `features/fullscreen/presentation/screens/`
- `features/imageimport/presentation/components/`
- `features/notes/presentation/components/NotesEditorDialog.kt`
- `features/search/presentation/components/CaptureSearchField.kt`
- `features/youtubeplayer/presentation/YoutubeEmbeddedPlayer.kt`

---

**Nota final:** este handoff é uma especificação funcional para orientar a identidade visual. Antes de aprovar telas finais, validar os estados e fluxos em um dispositivo Android com o build atual. O redesign deve mudar a apresentação, não os contratos de produto, persistência, privacidade ou navegação.
