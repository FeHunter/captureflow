# Backup

Feature responsável por exportar e importar o caderno local do CaptureFlow.

## Formato v1
- Arquivo ZIP com `manifest.json` e payloads em `images/`.
- Manifesto versionado, com referências, categorias, estado de conclusão, subtarefas, mídias e anotações.
- Imagens são copiadas para o arquivo e restauradas no armazenamento privado do novo dispositivo; URIs do dispositivo de origem não são reutilizadas.
- Seleção de destino/origem pelo Storage Access Framework do Android.

## Política de mesclagem
- Nunca apaga ou substitui registros locais.
- Categorias com nome já existente preservam a categoria e a ordenação locais; categorias ausentes são adicionadas.
- Referências equivalentes (mesma URL normalizada, título e conteúdo) são ignoradas.
- Conflito de ID com conteúdo diferente gera novo ID para a referência importada.
- IDs de subtarefas e mídias em conflito são regenerados; vínculos são remapeados para o novo ID da referência.
- A inserção de categorias, referências, subtarefas e mídias ocorre em uma única transação Room.
- Falha na importação remove os arquivos de imagem temporários já extraídos.

## Segurança e limites
- Apenas arquivos ZIP com manifesto CaptureFlow v1 são aceitos.
- Entradas com caminhos absolutos, traversal ou nomes fora de `manifest.json` e `images/` são rejeitadas.
- Limites: 10 MB para manifesto, 25 MB por imagem e 250 MB para o conteúdo descompactado.
- Imagens são deduplicadas no ZIP pelo SHA-256.
- O backup não inclui dados de Newsletters nem preferências do aplicativo nesta versão.

## Validação
- Testar exportação/importação com tarefas sem imagem, capas, galeria, anotações com imagem, categorias, subtarefas e estados concluídos.
- Testar reimportação do mesmo backup, colisão de IDs, duplicatas sem colisão de ID, arquivo inválido, versão desconhecida, imagem ausente e arquivo acima dos limites.
