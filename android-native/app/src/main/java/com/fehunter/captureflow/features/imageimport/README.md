# Image Import

Feature responsável pela entrada de conteúdo externo e processamento de imagens.

- `domain/gateway/ImageImportGateway.kt`: contrato sem tipos Android.
- `data/ImageImportGatewayImpl.kt`: adaptação para OCR, metadados e armazenamento.
- `data/storage/ImageImportStorageService.kt`: cópia para armazenamento privado, download de capas e exclusão best-effort da screenshot original; delega a remoção genérica de arquivos ao contrato Core.
- `data/metadata`: extração best-effort de metadados de links; URLs do YouTube consultam primeiro o endpoint oficial oEmbed para título e thumbnail, sem exigir chave de API.
- `data/ocr/OcrService.kt`: reconhecimento local via ML Kit e adaptação dos resultados para o domínio.
- `domain/ocr/TitleDetectionService.kt`: seleção testável do título por geometria, área, posição, confiança e perfil aproximado da interface.
- `docs/features/image-import/ocr-title-detection.md`: critérios, limitações e casos de teste da detecção de títulos.
- `data/android/ScreenshotMonitor.kt`: observa o MediaStore somente enquanto a MainActivity está em primeiro plano; não usa serviço, notificações ou execução em segundo plano.
- `presentation/model`: estados de importação e OCR.
- `presentation/viewmodel`: compartilhamento, prévia de links, revisão de imagens, confirmação por categoria, cópia privada e OCR.
- `presentation/components/ImageCaptureScreen.kt`: câmera própria com CameraX, foco por toque e captura temporária.
- `presentation/components/ImageCropReviewScreen.kt`: etapa de recorte da foto (original, 1:1, 4:5 e 16:9) antes do OCR/revisão da captura.

Fotos feitas pela câmera permitem ajustar o foco tocando no preview e escolher o enquadramento após a captura. O recorte é aplicado localmente e só então a imagem segue para compressão e OCR. O perfil de compressão escolhido (Médio/Alto) é aplicado antes de armazenar imagens da câmera ou Photo Picker; o perfil Alto também é usado em imagens de notas. Imagens escolhidas no Photo Picker e imagens recebidas pelo Sharesheet continuam seguindo o fluxo de revisão atual. O OCR sugere o título, o usuário pode editá-lo e escolher uma categoria antes de persistir. Cancelar remove somente arquivos temporários pertencentes ao app.

A confirmação do link continua explícita antes de persistir. Links YouTube usam o oEmbed/HTML para título e thumbnail; quando a imagem está disponível, ela é baixada para armazenamento privado e salva como capa, assim como nos demais links. O player oficial incorporado continua sendo responsabilidade da feature Fullscreen, que pode exibir o vídeo enquanto cards e outros componentes usam a capa persistida. A importação manual não apaga a imagem original; limpeza de screenshot só ocorre no fluxo opt-in após cópia privada bem-sucedida.
