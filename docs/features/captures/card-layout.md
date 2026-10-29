# Card Layout — Brief de Design UX/UI

## Objetivo

Criar um layout profissional, consistente e orientado à experiência do usuário para os cartões do CaptureFlow. O layout atual em grade não ficou visualmente equilibrado; este documento define o problema, os requisitos funcionais e visuais e os critérios para uma proposta de design.

O resultado deve ser simples de escanear, organizado, visualmente leve e utilizável em telas Android pequenas e grandes. A proposta deve priorizar hierarquia visual, legibilidade, alvos de toque adequados e uso eficiente do espaço, sem esconder informações importantes.

## Contexto do produto

CaptureFlow é um aplicativo Android nativo em Kotlin e Jetpack Compose para guardar e organizar referências capturadas de imagens/screenshots, textos e links. A biblioteca principal permite pesquisar e filtrar cartões. O processamento de OCR de imagens acontece localmente no dispositivo.

Os cartões persistem no banco Room. Uma referência pode ter uma imagem de capa e uma sub-checklist. Tarefas podem conter screenshot, título, descrição opcional e estado de conclusão. A imagem deve continuar acessível mesmo quando a miniatura não puder ser carregada; nesse caso, o cartão precisa manter uma apresentação alternativa sem quebrar a grade.

## Objetivo principal da tela

A biblioteca deve mostrar **duas colunas de cartões lado a lado**, repetidas em linhas conforme a quantidade de itens. O design deve funcionar em diferentes larguras de tela, evitando cartões estreitos demais, overflow, títulos cortados sem alternativa e controles difíceis de tocar.

O design deve propor medidas e regras adaptáveis:
- Duas colunas em dispositivos Android de largura comum, conforme solicitado.
- Espaçamento consistente entre cartões e nas bordas da tela.
- Alturas visuais equilibradas por linha, sem forçar conteúdo a caber de forma ilegível.
- Em telas muito estreitas ou com escala de fonte elevada, documentar a estratégia responsiva para preservar a legibilidade e a acessibilidade.
- A rolagem da biblioteca deve permanecer natural; não criar rolagem aninhada desnecessária dentro dos cartões.

## Conteúdo do cartão de referência

Definir uma hierarquia visual clara, preferencialmente nesta ordem:

1. **Imagem de capa/thumbnail**, quando existir. Deve ter proporção consistente e ocupar espaço previsível, sem dominar o cartão. A miniatura pode usar recorte para compor a grade, desde que a visualização em tela cheia permita ver a imagem inteira.
2. **Título principal**, com tamanho visual forte e limite de linhas definido para manter os cartões compactos. Se o título for maior que o limite, aplicar reticências.
3. **Título completo em texto secundário**, abaixo do título principal, para que nenhuma informação seja perdida. Deve ficar menor e visualmente subordinado ao título principal, mas continuar legível.
4. **Tag/categoria**, apresentada como chip compacto ou elemento equivalente. Evitar que a tag ocupe mais espaço que o título.
5. **Data e hora de inserção**, exibidas em formato localizado e compacto, por exemplo `29/10/2026 · 14:35`. O valor deve vir do campo persistido `createdAt`, não do horário atual da renderização. Usar o fuso horário local do dispositivo e manter um formato consistente.
6. **Estado de conclusão**, usando checkbox ou controle equivalente com alvo de toque adequado e rótulo acessível. A conclusão não pode ser comunicada apenas por cor.
7. **Ação de excluir**, claramente identificável, mas visualmente secundária em relação ao título e à imagem. Considerar confirmação ou desfazer quando aplicável para reduzir exclusões acidentais.
8. **Estado de processamento/erro OCR**, apenas quando relevante. Durante processamento, mostrar indicador discreto e o texto curto “Analisando imagem…”. Em erro, exibir mensagem breve sem apagar o conteúdo já salvo.

### Regras de conteúdo para cartões de referência

- Não exibir o texto OCR bruto, descrição extensa ou conteúdo integral no cartão de referência. A biblioteca deve permanecer visualmente escaneável.
- Não adicionar botão manual de análise OCR no cartão de referência: a análise de imagem inicia automaticamente quando a imagem é selecionada ou compartilhada para referências.
- A imagem, quando presente, pode ser tocada para abrir a visualização em tela cheia.
- Se não houver imagem, apresentar um estado alternativo bem desenhado (ícone/ilustração simples e área visual consistente), sem deixar um espaço vazio grande.
- O estado de erro ou imagem indisponível não deve apagar o título, a tag, a data/hora ou as ações.

## Conteúdo do cartão de tarefa

O cartão de tarefa deve compartilhar a linguagem visual dos cartões de referência, com hierarquia adaptada à execução:

- Checkbox/estado de conclusão.
- Título principal com limite de linhas e reticências.
- Título completo em texto menor, quando o título exceder o limite principal.
- Tag/categoria selecionável.
- Data e hora de inserção, a partir de `createdAt`.
- Screenshot opcional em miniatura.
- Descrição opcional, apresentada de forma resumida e sem tornar a grade excessivamente alta.
- Ação “Analisar screenshot” quando houver imagem e essa ação estiver disponível.
- Ação de excluir.
- Para itens concluídos, usar diferenciação visual sutil (por exemplo, texto atenuado e estado marcado), preservando contraste e legibilidade.

## Tags/categorias

Usar exatamente estas tags no novo design:

- **Ler depois**
- **Ideias**
- **Tarefa**
- **Importante**
- **Mais tarde**

A seleção de categoria deve usar um dropdown/menu acessível, com indicação clara da categoria atual. Os nomes devem manter a grafia consistente em filtros, cartões e formulários. A opção “Todas” é um filtro da biblioteca, não uma tag atribuída a um cartão.

O layout deve prever nomes de tag que ocupem larguras diferentes. Evitar chips cortados ou que empurrem as ações para fora do cartão. “Importante” pode receber uma ênfase visual controlada, mas não deve ser o único estado distinguível por cor.

## Sub-checklist

Cada cartão de referência pode conter uma sub-checklist persistente. Para manter a grade compacta:

- Recolher a sub-checklist por padrão.
- Mostrar um controle expansível com rótulo “Sub-checklist” e progresso, por exemplo “2/4 concluídas”.
- Ao expandir, mostrar os itens com checkbox, título e ação de exclusão.
- Manter um campo de adicionar subtarefa e uma ação clara para adicioná-la.
- Garantir que a expansão de um cartão não sobreponha ou esconda outros cartões; o layout da grade deve recalcular a altura da linha.
- O estado expandido deve ser visualmente óbvio e acessível por toque e leitor de tela.

## Visualização em tela cheia

Ao tocar na imagem, abrir uma visualização dedicada que:

- Exiba o título completo sem truncamento.
- Mostre a imagem inteira, preservando sua proporção original e usando ajuste do tipo “fit/contain”, sem cortar conteúdo.
- Tenha um botão de fechar claramente visível.
- Mantenha contraste adequado para o título e os controles, inclusive sobre imagens claras.
- Permita que imagens altas ou largas caibam na área disponível, com rolagem somente se for necessária para conteúdo adicional.

O título completo deve permanecer visível mesmo quando for longo. A visualização em tela cheia não deve reaproveitar o recorte da miniatura.

## Paleta de cores

A interface deve respeitar o tema claro/escuro já existente no CaptureFlow. Não introduzir uma nova identidade visual independente dos componentes existentes.

### Paleta atualmente usada no código do app

| Uso | Claro | Escuro |
|---|---|---|
| Fundo da tela | `#F4F8FC` | `#07182B` |
| Superfície/cartão | `#FFFFFF` | `#0C2B46` |
| Texto principal | `#07182B` | `#F3F7FB` |
| Texto secundário | `#3D5D73` | `#B7CEDF` |
| Cor primária/ações | `#1F5F8B` | `#63C4F0` |
| Cor secundária | `#164B62` | `#58B9D6` |
| Destaque/terciária | `#39A6D6` | `#8ABFE8` |
| Superfície alternativa | `#E1EDF6` | `#164B62` |
| Contorno | `#7D9AAF` | `#52758E` |

### Referência visual complementar registrada anteriormente

Também foi usada como referência visual a combinação:
- Fundo: `#F7F5FF`
- Matiz de cabeçalho/texto: `#29213D`

Essas duas cores devem ser tratadas como referência complementar de design, não como substituição automática da paleta atualmente configurada no código. A proposta deve indicar claramente se recomenda manter a paleta atual ou propor uma evolução, justificando a decisão.

### Diretrizes de cor

- Evitar cartões com bordas, sombras ou cores muito fortes em todos os elementos ao mesmo tempo.
- Usar superfície, espaçamento e tipografia para criar hierarquia antes de adicionar decoração.
- Manter contraste legível para texto principal, metadados, chips, checkbox e ícones.
- O tema escuro deve usar azuis-marinho/oceano, não preto puro.
- Não comunicar conclusão, erro ou importância apenas pela cor.
- Aplicar a cor de destaque com moderação, sobretudo em tags e ações.

## Diretrizes de estilo e interação

- Estética profissional, moderna, minimalista e coerente com o tema azul-marinho/oceano/ciano atual.
- Priorizar uma composição orientada à imagem quando existir capa, mas manter título, tag e data/hora fáceis de localizar.
- Usar cantos, espaçamentos, elevação e bordas de forma consistente com Material 3.
- Reduzir ruído visual: evitar muitos botões preenchidos, divisórias desnecessárias e ícones sem função clara.
- Alvos de toque confortáveis; ações secundárias não devem competir com o título.
- Não depender apenas de gestos para ações importantes.
- Preservar suporte a tamanhos de fonte maiores, leitores de tela e contraste adequado.
- A grade deve continuar agradável quando um cartão tem imagem e outro não, ou quando um cartão tem título/tag longos.

## O que o designer deve entregar

1. Uma proposta visual de cartão de referência e uma de cartão de tarefa.
2. Variações para: com imagem, sem imagem, título longo, tag longa, OCR em processamento, erro OCR, concluído e sub-checklist expandida/recolhida.
3. Uma composição da biblioteca com **duas colunas** e pelo menos duas linhas de cartões, incluindo alturas e espaçamentos.
4. Uma proposta de visualização em tela cheia com título completo e imagem integral.
5. Estados para tema claro e tema escuro usando a paleta documentada.
6. Especificação de medidas: padding, espaçamento entre cartões, proporção/altura da miniatura, limite de linhas, tipografia, raio, elevação, tamanhos dos ícones e alvos de toque.
7. Anotações de comportamento para títulos longos, subtarefas, tags, datas e estados de carregamento/erro.
8. Componentes e variantes organizados para implementação consistente em Jetpack Compose/Material 3.

## Critérios de aceite

- [ ] A biblioteca apresenta cartões em grade de duas colunas, lado a lado, com espaçamento consistente.
- [ ] A composição é equilibrada, profissional e fácil de escanear em uma tela Android comum.
- [ ] O título principal tem limite de linhas e reticências; o título completo aparece abaixo em tamanho menor.
- [ ] A tag é visível e selecionável sem dominar o cartão.
- [ ] Data e hora de inserção são exibidas a partir de `createdAt`, em formato local consistente.
- [ ] Imagens têm miniaturas consistentes sem perder acesso à imagem completa.
- [ ] Tocar na imagem ou no título abre detalhes em tela cheia, com ícone de expansão visível.
- [ ] A tela de detalhes mantém título, data/hora e categoria no cabeçalho; a imagem ocupa todo o espaço restante sem cortes.
- [ ] O botão de sub-checklist fica entre concluir e excluir, mesmo com zero subtarefas, e abre um modal com ações de adicionar, concluir e excluir.
- [ ] Estados de OCR, conclusão, erro e ausência de imagem estão contemplados.
- [ ] O design funciona nos temas claro e escuro e mantém contraste/acessibilidade.
- [ ] O design respeita as cinco tags definidas neste documento.
- [ ] O cartão não exibe texto OCR bruto nem conteúdo longo que prejudique a leitura rápida da grade.

## Notas para implementação

- Campo de data/hora disponível: `CaptureEntity.createdAt` e `ChecklistTaskEntity.createdAt` (timestamp em milissegundos).
- Referências: `CaptureEntity` inclui `title`, `category`, `imageUri`, `isCompleted` e `createdAt`.
- Tarefas: `ChecklistTaskEntity` inclui `title`, `category`, `imageUri`, `description`, `isCompleted` e `createdAt`.
- Sub-checklist: `CaptureSubtaskEntity` inclui `title`, `isCompleted` e `createdAt`.
- Implementação nativa: Kotlin + Jetpack Compose + Material 3; os cartões devem poder ser implementados como componentes reutilizáveis com variantes.
- Este documento é um briefing de design, não uma afirmação de que a implementação visual já foi aprovada ou validada em dispositivo.


## Atualização de interação — detalhes e sub-checklist

- Tocar na miniatura **ou no título** abre uma visualização de detalhes em tela cheia.
- O ícone de expansão no canto superior da miniatura comunica visualmente que o cartão pode ser aberto.
- A tela de detalhes mantém no cabeçalho o título completo, data/hora e categoria. O corpo é dedicado à imagem, que ocupa toda a área restante com ajuste `Fit/contain`, preservando a proporção e sem cortes. Não exibir conteúdo capturado nem URL no detalhe em tela cheia.
- O texto secundário do cartão pode ocupar até duas linhas e recebe altura de 38–44 dp para evitar que a tipografia fique comprimida. A altura de referência do cartão é 320 dp.
- A barra inferior mantém a ordem **concluir → sub-checklist/progresso → excluir**. O botão de sub-checklist fica visível mesmo quando o cartão ainda não tem subtarefas, para permitir adicionar a primeira.
- A sub-checklist abre em modal bottom sheet, onde é possível adicionar, concluir e excluir subtarefas. O progresso usa o formato concluídas/total.


## Atualização de layout — cartão ampliado e detalhe focado na imagem

- Altura de referência do cartão na grade: **320 dp** (antes: 300 dp).
- O subtítulo mantém até duas linhas, com altura reservada de **38–44 dp** (antes: 28–34 dp), para melhorar a leitura e evitar compressão.
- A grade permanece em duas colunas, com 12 dp de espaçamento e ações inferiores de 48 dp.
- A visualização em tela cheia é dividida em duas regiões:
  1. **Cabeçalho:** título completo, data/hora persistida e tag/categoria; botão de fechar alinhado à direita.
  2. **Corpo:** imagem ocupando toda a área vertical restante, usando ajuste `ContentScale.Fit` para preservar a imagem inteira sem recorte.
- O cabeçalho não rola junto com a imagem. Não exibir conteúdo OCR capturado ou URL no modo de visualização da imagem.
- Se não houver imagem disponível, manter a área principal e apresentar um placeholder centralizado, sem quebrar a composição.
