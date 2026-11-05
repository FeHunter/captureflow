# Image Compression

Pipeline local adaptativo para imagens raster recebidas por URI file/content ou bytes codificados.
- Dimensão máxima 2560 px; orientação EXIF normalizada.
- Imagem opaca: gerar WebP lossy e JPEG e persistir o menor arquivo válido.
- PNG e imagens com transparência: WebP lossless para preservar detalhes/alpha; PNG lossless é fallback se o encoder WebP do dispositivo falhar. Fotografias JPEG/opaque usam comparação WebP lossy vs JPEG.
- API 30+: WEBP_LOSSY/WEBP_LOSSLESS; API 26–29: WEBP legado com qualidade 0–100.
- Perfil Médio: JPEG 70 / WebP 78. Alto: JPEG 52 / WebP 66. Valores são parâmetros de encoder, não percentuais de economia.
- Saída em storage privado files/captures; extensão corresponde ao encoder escolhido (WebP/JPEG; PNG lossless como fallback de transparência). Decodificação Android usa conteúdo real, não extensão.
- Image Import aplica compressão a imagens selecionadas e downloads de preview; Notes envia recorte PNG em memória como ByteArray e usa Alto; troca de capa usa o perfil atual.
- Arquivos antigos não são convertidos automaticamente para evitar risco de perda/URI quebrada; novos imports e capas usam o pipeline atualizado.
- A origem nunca é apagada pelo compressor. Falha mantém a origem como fallback, exceto download de thumbnail que falha sem persistir arquivo cru.
