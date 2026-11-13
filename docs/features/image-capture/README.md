# Image Capture — câmera e importação de imagens

**Status:** implementado como parte da feature proprietária Image Import; não é feature autônoma.

## Fluxo atual
- Biblioteca oferece Fotografar (CameraX), Galeria (Photo Picker) e Nova tarefa (texto).
- CameraX usa preview próprio, foco por toque e captura temporária. No fluxo da Biblioteca, a foto segue para revisão/recorte (Original, 1:1, 4:5, 16:9).
- Notes reutiliza o mesmo CameraX com retorno da foto temporária sem revisão intermediária; o editor Notes possui seu próprio recorte e compressão.
- Photo Picker e Sharesheet convergem para revisão, sem necessariamente passar pelo recorte da câmera.
- OCR ML Kit local sugere título editável; usuário escolhe categoria válida e confirma antes de persistir.
- Perfil de compressão de Settings é aplicado antes do OCR/persistência.
- Imagens são copiadas para storage privado quando possível; URI original é fallback.
- Cancelar remove somente arquivos temporários do app; nunca apagar imagem escolhida manualmente.
- Monitoramento de screenshots é separado, opt-in e baseado em MediaStore/notificação.

## Ownership
ImageCaptureScreen, ImageCropReviewScreen, ImageImportViewModel e ImageImportGateway pertencem a Image Import. Persistência de referência é delegada aos UseCases públicos de Captures. Notes consome a câmera como entrada de mídia, sem duplicar CameraX.

## Validação
Testar permissão/câmera, foco, captura, recorte, Photo Picker, Sharesheet, OCR vazio/erro, compressão, cancelamento, categoria obrigatória e persistência após reinício. Validar CI e dispositivo no commit atual.
