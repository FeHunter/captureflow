# Backup e restauração

**Formato atual:** ZIP v2. Exportação usa Storage Access Framework e inclui manifest.json e images/{sha256}.bin.

## Dados exportados
- Captures: título, conteúdo, tipo, categoria, conclusão, timestamps, URL, capa, mídias, subtarefas e notas.
- Categorias e ordem.
- Imagens da capa, galeria e Notes, deduplicadas por SHA-256.
- Newsletters: fontes (feedUrl, title, siteUrl, description, imageUrl, createdAt, fetch state/ETag/Last-Modified/erro) e artigos (stableId, canonicalUrl, conteúdo/resumo, imagem URL, publicado/fetched, isRead e savedCaptureId).

## Compatibilidade e mesclagem
- Exporta versão 2; importa versões 1 e 2. Arquivos v1 não possuem arrays Newsletter e continuam válidos.
- Importação aditiva; nunca apaga/sobrescreve dados locais.
- Categorias existentes preservam ordem local; ausentes são adicionadas.
- Duplicatas de Capture são ignoradas pela identidade existente; IDs conflitantes são remapeados.
- Newsletter source é associada por feedUrl; se já existir, preserva o registro local. Artigos são inseridos com estratégia IGNORE e índices únicos (sourceId+stableId/canonicalUrl), preservando artigo local existente.
- savedCaptureId é remapeado para o Capture importado ou existente; vínculo inexistente vira null.
- Captures continuam importados em transação Room. Newsletter usa repositório próprio e importação aditiva após o lote de Captures; testar recuperação em falha parcial.
- URIs originais não são reutilizadas; imagens restauradas em storage privado.

## Limites
Manifesto 10 MB; imagem 25 MB; descompactado 250 MB; 50.000 registros e 50.002 entradas. v2 inclui dados Newsletter, mas não inclui preferências Settings nem imagem de feed baixada (Newsletter mantém imageUrl remota).

## Validação
Testar export/import v1 e v2, feed duplicado, source ID conflitante, artigo duplicado por stableId/URL, savedCaptureId remapeado, artigos lidos/não lidos, mídia/Notes e falha parcial. CI e dispositivo ainda pendentes nesta branch.


## Relação com o backup do Android

- O Android Auto Backup e a transferência automática de dados do app estão desabilitados no Manifest.
- O backup do CaptureFlow é exclusivamente manual, em arquivo ZIP escolhido pelo usuário pelo Storage Access Framework.
- Para migrar dados entre dispositivos, exportar o ZIP no dispositivo de origem e importá-lo manualmente no destino.
- O usuário é responsável por manter cópia segura do arquivo; o app não envia backups aos seus servidores.
