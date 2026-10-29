# UI/UX atual — Captures

## Navegação
- Tela principal única: **Referências**; sem bottom navigation e sem tela independente de tarefas.
- Top bar com `CaptureFlow v{BuildConfig.VERSION_NAME}` e menu de tema: seguir dispositivo, claro, escuro.
- Conteúdo em rolagem vertical: seletor/monitoramento de imagem, busca, filtros e grade de referências.

## Entrada de conteúdo
- Não há formulário manual de texto/link; usar compartilhamento Android.
- Photo Picker permite selecionar uma imagem.
- Ações de monitoramento explicam/ativam/desativam o serviço opcional de screenshots.
- O fluxo da notificação abre modal com prévia e confirmação. Confirmar salva a imagem como referência; cancelar não salva.

## Busca e filtros
- Busca por título e conteúdo persistido, aplicada junto com filtro de categoria.
- X de limpeza aparece apenas quando há texto.
- Chips horizontais: Todas, Ler depois, Ideias, Tarefa, Importante, Mais tarde.
- `Referência` é usada como categoria no fluxo de screenshot, mas ainda não está consistente com a lista visível.

## Cartão
- Duas colunas, altura fixa nominal de 320 dp.
- Capa de 104 dp, crop na grade; placeholder se não houver imagem.
- Título e texto secundário (no código atual ambos renderizam `capture.title`; provavelmente precisa de decisão/correção).
- Categoria em chip com menu; data/hora `dd/MM/yyyy · HH:mm`.
- Linha de ações: checkbox de conclusão, botão de sub-checklist com contagem e exclusão.
- Estado OCR mostra “Analisando imagem…” ou erro em faixa sobre a capa.
- Cartão concluído tem título atenuado/riscado.

## Detalhe fullscreen
- Cabeçalho: título, data/hora, categoria, atalho para sub-checklist e botão fechar.
- Imagem ocupa área restante e usa `ContentScale.Fit`.
- Swipe horizontal: esquerda próxima referência, direita anterior; sem loop e respeitando a lista fornecida ao cartão.
- `‹ Deslize para navegar ›` é um Surface sobreposto na parte inferior central da área da imagem, não elemento de layout que reserve espaço.

## Sub-checklist
Modal bottom sheet com título/progresso, itens com checkbox, exclusão e campo de nova subtarefa. O atalho fullscreen fecha o diálogo de imagem e abre a sub-checklist da referência corrente.

## Tema
Paleta atual azul-marinho/oceano/ciano; tema claro/escuro evita preto puro e usa cores do MaterialTheme. A seleção de tema deve manter preferência local e seguir sistema quando escolhido.

## Acessibilidade e validação
Manter alvos de toque de 48 dp, rótulos de ícone e estados não dependentes só de cor. Validar telas estreitas, fonte ampliada, contraste, gesto e leitura de imagem em dispositivo.


## Navegação e título no fullscreen

- Navegação por `HorizontalPager`: swipe-left avança; swipe-right retorna; a lista respeita busca/filtro ativos e não faz loop nas extremidades.
- Cabeçalho sincronizado com a página estabilizada do pager.
- Ação **Editar título** abre um diálogo apenas no fullscreen. O título é validado como não vazio e persistido no campo `captures.title` sem alterar os demais campos.
- A sub-checklist permanece acessível pelo botão do cartão; o atalho de sub-checklist no cabeçalho fullscreen foi removido.


## Navegação e edição do título

- `HorizontalPager` navega pela lista filtrada, sem wrap-around, e sincroniza o cabeçalho com a página estabilizada.
- A ação **Editar título** aparece apenas no fullscreen. O diálogo valida texto não vazio e persiste apenas `captures.title`.
- O atalho de sub-checklist foi removido do cabeçalho fullscreen; o botão de sub-checklist no cartão continua ativo.


## Contraste do título e voltar nativo
- O título sobre a imagem usa um degradê preto mais opaco na região superior, esmaecendo gradualmente para transparência; não alterar proporção, enquadramento da imagem ou hierarquia visual.
- O botão/gesto nativo de voltar fecha primeiro o visualizador da referência; ao voltar novamente, fecha a categoria expandida e retorna à biblioteca.
- Os botões visíveis de fechar/voltar continuam disponíveis.


## Navegação fullscreen por cards e nome do app (2026-10-29)
- O nome visível do produto é **Capture Flow** na barra superior e no rótulo do aplicativo Android; “Biblioteca” não deve substituir o nome do produto.
- Ao abrir uma captura por um thumbnail, o visualizador permite swipe-left para a próxima captura da mesma categoria filtrada e swipe-right para a anterior, sem loop nas extremidades.
- A data, categoria, conclusão e imagem acompanham a captura selecionada no pager. A dica de swipe permanece sobre a imagem e o botão/gesto nativo Back continua fechando o visualizador sem fechar a categoria por baixo.


## Contrato de compartilhamento de links (2026-10-29)
- Ao receber URL via Android Sharesheet, o ViewModel prepara uma captura pendente e a UI abre o diálogo **Salvar link como referência**.
- O diálogo mostra o título extraído, URL, descrição quando encontrada e quatro opções exclusivas: Ler depois, Verificar, Salvar, Importante.
- Uma categoria precisa estar selecionada e a requisição de metadados precisa terminar para habilitar **Salvar referência**.
- Cancelar descarta o estado pendente e não persiste a referência. Se o fetch falhar, o usuário ainda pode salvar com o título fallback.
- O parser HTTP lê até 512 KB do HTML; prioriza H1, depois metadados e title. Para imagem, prioriza img dentro de article/main, depois og:image/Twitter. Não executa JavaScript.
- Após confirmar, salva título, URL/conteúdo e categoria; a imagem, quando disponível, é baixada para armazenamento privado e usada como capa.
- Texto compartilhado sem URL continua sendo salvo diretamente; compartilhamento de imagem segue o fluxo de importação/OCR.
