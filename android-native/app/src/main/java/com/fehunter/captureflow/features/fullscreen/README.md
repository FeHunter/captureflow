# Fullscreen

Feature responsável pela experiência ampliada de uma referência.

- `FullscreenCaptureDialog`: navegação paginada entre referências da categoria, link original, conclusão, edição de título, anotações, sub-checklist por referência e exclusão.
- `FullscreenCapturePage`: apresentação da imagem em tela cheia, título e dica de navegação.
- O estado persistente continua vindo do fluxo de Captures por callbacks; a feature não mantém uma segunda fonte de verdade.
- O carregamento da imagem usa `core/ui/ImageBitmapLoader`.

O overlay de anotações aparece no canto inferior esquerdo quando existe conteúdo salvo.

A sub-checklist usa `CaptureSubtask` e callbacks do `CaptureViewModel`; os itens permanecem vinculados a `captureId` e são removidos em cascata ao excluir a referência.