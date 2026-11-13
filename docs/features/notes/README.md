# Notes

UI de Notes; dados pertencem ao agregado Capture em Captures. Blocos ordenados com texto até 10.000 caracteres e imagem opcional. Botão balão cria; toque longo edita/remove; toque simples na imagem abre preview fullscreen. Recortes Original/1:1/4:5/16:9. Photo Picker faz staging privado; recorte PNG lossless em memória e compressão Alto para JPEG privado. URI final só persiste após sucesso; falha mantém seleção. CaptureNote/Codec em Captures, formato versionado em captures.notes; Notes não acessa Room.

## Entrada de imagem

O editor oferece Galeria e Câmera lado a lado. A câmera reutiliza o CameraX de Image Import, retornando a foto temporária sem abrir uma segunda tela de recorte; o próprio editor Notes executa o recorte, staging privado e compressão Alto. O arquivo temporário da câmera é removido após o staging ou quando o editor é descartado. Cancelar o recorte não substitui a imagem já anexada.

## Fullscreen

O painel de notas exibido no Fullscreen possui controle Expandir/Recolher quando existem blocos. A expansão altera somente o espaço visível/scroll do painel; não modifica nem reordena notas persistidas.
