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
