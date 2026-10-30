# Image Import

Feature responsável pela entrada de conteúdo externo e processamento de imagens.

- `domain/gateway/ImageImportGateway.kt`: contrato sem tipos Android.
- `data/ImageImportGatewayImpl.kt`: adaptação para OCR, metadados e armazenamento.
- `data/storage/ImageImportStorageService.kt`: cópia para armazenamento privado, download de capas e exclusão best-effort da screenshot original; delega a remoção genérica de arquivos ao contrato Core.
- `data/metadata`: extração best-effort de metadados de links.
- `data/ocr`: análise local via ML Kit.
- `data/android`: monitoramento e notificações de screenshots.
- `presentation/model`: estados de importação e OCR.
- `presentation/viewmodel`: compartilhamento, prévia de links, confirmação por categoria, cópia privada e OCR.

A confirmação do link continua explícita antes de persistir. A importação manual não apaga a imagem original; limpeza de screenshot só ocorre no fluxo opt-in após cópia privada bem-sucedida.
