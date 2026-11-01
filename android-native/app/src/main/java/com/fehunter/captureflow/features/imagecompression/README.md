# Image Compression

Feature responsável pela compressão local de imagens antes do armazenamento privado.

## Perfis
- **Médio:** JPEG quality 70.
- **Alto:** JPEG quality 50.
- A maior dimensão é limitada a 2560 px; orientação EXIF é aplicada antes da codificação.
- Os valores indicam qualidade JPEG, não prometem uma redução percentual exata no tamanho final do arquivo.

## Integração
- Image Import usa o perfil selecionado em Settings para imagens da câmera e Photo Picker, antes do OCR e da persistência.
- Notes usa sempre o perfil Alto para imagens anexadas aos blocos de pensamento. O recorte é codificado como PNG lossless em memória e enviado como `ByteArray`, sem arquivo intermediário ou URI `file://`.
- O serviço não apaga a origem. Image Import mantém seu fallback de URI original quando a cópia/compressão falha; Notes só atualiza a anotação depois de receber a URI privada comprimida.
- O processamento é local e os arquivos comprimidos ficam no armazenamento privado do app.

## Implementação
- domain/model/ImageCompressionProfile.kt: perfis e qualidade JPEG.
- domain/service/ImageCompressor.kt: contrato sem dependência Android, com entradas URI e bytes raster codificados.
- data/ImageCompressionService.kt: implementação Android com leitura `file://`/`content://`, fallback por `ParcelFileDescriptor`, inspeção de dimensões antes da decodificação, correção EXIF, limite de 2560 px, tratamento de transparência e escrita JPEG privada; instanciada pelo AppContainer.


## Formatos e leitura de origem

- A entrada pode ser uma URI Android (`file://` ou `content://`) ou bytes raster codificados em memória. Para URIs `file://` internas, o serviço lê diretamente com `FileInputStream`; para URIs `content://`, usa `ContentResolver` e tenta `openFileDescriptor` quando o stream falha ou retorna nulo. Notes usa a entrada por bytes para não depender de um arquivo/URI intermediário após o recorte.
- O decoder Android suporta BMP, GIF, JPEG, PNG e WebP. HEIF/HEIC depende da versão Android (Android 9+) e do dispositivo; AVIF depende do Android 12+ para decodificação de 8 bits.
- A saída do serviço é sempre JPEG em armazenamento privado, independentemente do formato de entrada. URI e bytes passam pelo mesmo encoder, limite de dimensão e tratamento de transparência. A qualidade configurada (70 ou 50) é o parâmetro de codificação JPEG, não uma porcentagem garantida de redução do arquivo.
- SVG, PDF e formatos RAW não são entradas raster convencionais deste pipeline e não devem ser tratados como imagens compatíveis sem um decoder específico.
