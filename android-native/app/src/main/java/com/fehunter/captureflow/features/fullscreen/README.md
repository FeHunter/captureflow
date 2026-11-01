# Fullscreen

Feature responsável pela experiência ampliada de uma referência.

- `FullscreenCaptureDialog`: navegação paginada entre referências da categoria, link original no cabeçalho e na coluna de ações, conclusão, edição de título e categoria no mesmo modal, abertura do editor de anotações, sub-checklist por referência e exclusão.
- `FullscreenCapturePage`: apresentação da imagem em tela cheia, título e dica de navegação.
- O estado persistente continua vindo do fluxo de Captures por callbacks; a feature não mantém uma segunda fonte de verdade.
- O carregamento da imagem usa `core/ui/ImageBitmapLoader`.

O botão de anotações fica no canto inferior esquerdo e usa um ícone de balão de conversa. A edição é delegada à feature `notes`; o fullscreen não mantém estado de edição.

A sub-checklist usa `CaptureSubtask` e callbacks do `CaptureViewModel`; os itens permanecem vinculados a `captureId` e são removidos em cascata ao excluir a referência.

## Estado concluído no fullscreen
- Referências concluídas exibem o componente compartilhado `CompletedBadge` sobre a imagem, no canto superior direito.
- Quando `isCompleted` é verdadeiro, a coluna vertical de ações mostra somente o checkbox para reabrir a referência. As ações de sub-checklist, link, edição e exclusão ficam disponíveis novamente quando a referência volta ao estado pendente.
- O botão de anotações no canto inferior esquerdo é independente da coluna vertical e mantém o comportamento atual.
