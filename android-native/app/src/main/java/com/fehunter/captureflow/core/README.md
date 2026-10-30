# Core

Código transversal sem ownership de feature.

- `storage/PrivateImageStorage.kt`: contrato mínimo para remoção de imagens privadas, consumido por Captures.
- `storage/LocalPrivateImageStorage.kt`: implementação genérica que só remove arquivos dentro do diretório privado do app.
- `ui/ImageBitmapLoader.kt`: carregamento de imagem com decodificação limitada.

Não mover regras específicas de Captures, Categories ou Image Import para Core.
