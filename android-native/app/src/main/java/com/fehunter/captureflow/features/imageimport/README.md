# Image Import

Feature responsável pela entrada de conteúdo externo e processamento de imagens.

- `domain/gateway/ImageImportGateway.kt`: contrato sem tipos Android.
- `data/ImageImportGatewayImpl.kt`: adaptação para OCR, metadados e armazenamento.
- `data/storage/ImageImportStorageService.kt`: cópia para armazenamento privado, download de capas e exclusão best-effort da screenshot original; delega a remoção genérica de arquivos ao contrato Core.
- `data/metadata`: extração best-effort de metadados de links.
- `data/ocr/OcrService.kt`: reconhecimento local via ML Kit e adaptação dos resultados para o domínio.
- `domain/ocr/TitleDetectionService.kt`: seleção testável do título por geometria, área, posição, confiança e perfil aproximado da interface.
- `docs/features/image-import/ocr-title-detection.md`: critérios, limitações e casos de teste da detecção de títulos.
- `data/android`: monitoramento e notificações de screenshots.
- `presentation/model`: estados de importação e OCR.
- `presentation/viewmodel`: compartilhamento, prévia de links, revisão de imagens, confirmação por categoria, cópia privada e OCR.
- `presentation/components/ImageCaptureScreen.kt`: câmera própria com CameraX; a foto permanece temporária até a confirmação.

Fotos feitas pela câmera, imagens escolhidas no Photo Picker e imagens recebidas pelo Sharesheet passam por uma revisão única. O OCR sugere o título, o usuário pode editá-lo e escolher uma categoria antes de persistir. Cancelar remove somente arquivos temporários pertencentes ao app.

A confirmação do link continua explícita antes de persistir. A importação manual não apaga a imagem original; limpeza de screenshot só ocorre no fluxo opt-in após cópia privada bem-sucedida.
