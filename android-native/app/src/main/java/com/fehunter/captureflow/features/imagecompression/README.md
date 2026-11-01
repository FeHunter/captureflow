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
- domain/service/ImageCompressor.kt: contrato público consumido por Image Import.
- data/ImageCompressionService.kt: implementação Android com decodificação limitada, orientação EXIF e escrita JPEG privada; instanciada pelo AppContainer.


## Formatos e leitura de origem

- A entrada é uma URI Android, não um caminho de arquivo obrigatório. Para URIs `file://` internas, o serviço lê diretamente com `FileInputStream`; para URIs `content://`, usa `ContentResolver` e tenta `openFileDescriptor` se o stream vier nulo.
- O decoder Android suporta BMP, GIF, JPEG, PNG e WebP. HEIF/HEIC depende da versão Android (Android 9+) e do dispositivo; AVIF depende do Android 12+ para decodificação de 8 bits.
- A saída do serviço é sempre JPEG em armazenamento privado, independentemente do formato de entrada. A qualidade configurada (70 ou 50) é o parâmetro de codificação JPEG, não uma porcentagem garantida de redução do arquivo.
- SVG, PDF e formatos RAW não são entradas raster convencionais deste pipeline e não devem ser tratados como imagens compatíveis sem um decoder específico.
