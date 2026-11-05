# Image Import

Entradas: Sharesheet texto/URL/imagem, Photo Picker, câmera CameraX e monitoramento opt-in de screenshot. Link fica pendente até revisão/confirmação de categoria; cancelar não persiste. YouTube tenta oEmbed oficial para título/thumbnail; outros sites usam H1/metadados e imagem article/main/body. Download de capa após confirmação.

OCR ML Kit on-device sugere título editável; erro não descarta imagem. Câmera tem foco por toque e recorte; Photo Picker/compartilhamento têm revisão própria. Copiar para storage privado quando possível; URI original fallback. Nunca apagar imagem escolhida manualmente. Screenshot discovery best-effort por MediaStore, sem ler pixels/Accessibility/overlay; exclusão da origem só após confirmação e cópia privada bem-sucedida. Parser não executa JS.

## Armazenamento de previews
Downloads de thumbnails passam pelo ImageCompressionService antes de serem retornados como URIs privadas. Se download, decodificação ou compressão falhar, não persistir o arquivo temporário original; manter o link como fallback. Imagens selecionadas usam cópia/compressão privada quando possível.
