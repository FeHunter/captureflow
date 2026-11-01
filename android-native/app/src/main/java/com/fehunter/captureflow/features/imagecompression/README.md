# Image Compression

Feature responsável pela compressão local de imagens antes do armazenamento privado.

## Perfis
- **Médio:** JPEG quality 70.
- **Alto:** JPEG quality 50.
- A maior dimensão é limitada a 2560 px; orientação EXIF é aplicada antes da codificação.
- Os valores indicam qualidade JPEG, não prometem uma redução percentual exata no tamanho final do arquivo.

## Integração
- Image Import usa o perfil selecionado em Settings para imagens da câmera e Photo Picker, antes do OCR e da persistência.
- Notes usa sempre o perfil Alto para imagens anexadas aos blocos de pensamento.
- O serviço não apaga a origem. A URI original permanece como fallback quando a decodificação/compressão falha.
- O processamento é local e os arquivos comprimidos ficam no armazenamento privado do app.

## Implementação
- domain/model/ImageCompressionProfile.kt: perfis e qualidade JPEG.
- data/ImageCompressionService.kt: decodificação limitada, orientação EXIF e escrita JPEG privada.
