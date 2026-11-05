# Contrato atual do banco — Captures

**Banco:** Room, arquivo local `captureflow.db`.  
**Versão Room:** 11.

## `captures`
- `id`: chave primária.
- `title`: título exibido no cartão.
- `content`: texto extraído ou conteúdo da referência.
- `kind`: classificação como `image`, `link` ou `text`.
- `category`: categoria da referência.
- `sourceUrl`: URL identificada no conteúdo, quando houver.
- `imageUri`: URI da imagem original/cópia privada, quando houver.
- `createdAt`: instante de criação.
- `isCompleted`: estado de conclusão.
- `previousCategory`: coluna legada mantida no schema por compatibilidade; é limpa pela migração 8→9 e não é usada pela regra atual de conclusão.
- `notes`: coleção de blocos serializada/versionada por CaptureNotesCodec; cada bloco aceita até 10.000 caracteres e imagem opcional.

## `capture_media`
- `id`: identificador estável da mídia.
- `captureId`: referência proprietária; chave estrangeira para `captures(id)` com exclusão em cascata.
- `uri`: URI do arquivo privado local.
- `mediaType`: MIME/type declarado para a mídia.
- `position`: ordem estável dentro da referência; índice único composto com `captureId`.
- `isCover`: indica a capa selecionada.
- `sourceUrl`: URL original da mídia, quando disponível.
- `createdAt`: instante de inclusão.

A migration 9→10 importa cada `captures.imageUri` não vazio como uma linha `capture_media` na posição 0, marcada como capa. A coluna legada `captures.imageUri` permanece no schema para compatibilidade e fallback; ela não é removida nesta etapa.

## `newsletter_sources`
- `id`: identificador da fonte.
- `title`: nome exibido.
- `feedUrl`: endereço RSS/Atom; índice único para evitar fontes duplicadas.
- `siteUrl`, `description`, `imageUrl`: metadados do canal.
- `createdAt`, `lastFetchedAt`: criação e última atualização.
- `etag`, `lastModified`: validadores HTTP condicionais.
- `lastError`: última falha registrada, quando houver.

## `newsletter_articles`
- `id`: identificador local do artigo.
- `sourceId`: fonte proprietária, com chave estrangeira e exclusão em cascata.
- `stableId`: GUID/ID do feed; único por fonte.
- `title`, `canonicalUrl`, `publishedAt`, `summary`, `contentHtml`, `imageUrl`: conteúdo disponível no feed.
- `isRead`: estado de leitura.
- `savedCaptureId`: referência da Biblioteca criada a partir do artigo, se houver.
- `fetchedAt`: instante de importação.

A retenção remove artigos mais antigos não salvos acima de 100 por fonte; artigos com `savedCaptureId` não são removidos pela limpeza automática.

## `checklist_tasks`
- `id`: chave primária.
- `title`: título da tarefa.
- `isCompleted`: estado de conclusão.
- `createdAt`: instante de criação.
- `imageUri`: URI opcional da imagem anexada.
- `description`: descrição opcional extraída/associada.

## `capture_subtasks`
- `id`: chave primária.
- `captureId`: identificador da referência proprietária.
- `title`: texto do sub-item.
- `isCompleted`: estado de conclusão.
- `createdAt`: instante de criação/ordenação.

## Migrações
- v1 → v2: adiciona `imageUri` às tarefas.
- v2 → v3: adiciona `description` às tarefas.
- v3 → v4: cria `capture_subtasks` e índice por `captureId`.
- v4 → v5: adiciona categoria às tarefas legadas.
- v5 → v6: cria categorias padrão.
- v6 → v7: alteração histórica que adicionou `previousCategory` e migrou referências concluídas para a categoria `Concluído`.
- v7 → v8: adiciona `notes` às referências.
- v8 → v9: restaura a categoria original dos registros que estavam em `Concluído` usando `previousCategory` (fallback `Salvar`), limpa a coluna legada e remove a categoria `Concluído` da tabela `categories`.
- v9 → v10: cria `capture_media`, índices por `captureId` e por (`captureId`, `position`), e migra `imageUri` legado para a nova coleção como capa.
- v10 → v11: cria `newsletter_sources` com índice único por `feedUrl` e `newsletter_articles` com FK em `sourceId`, índices e deduplicação por GUID/ID e URL canônica.

Ao excluir uma referência, o repositório remove subtarefas e mídias relacionadas dentro de transação; a chave estrangeira também define `ON DELETE CASCADE` para `capture_media`. A limpeza física de arquivos privados é responsabilidade da camada de aplicação/armazenamento.

A conclusão é independente da categoria: `isCompleted` é o único estado de conclusão. Marcar ou reabrir uma referência não altera `category`. A coluna `previousCategory` permanece apenas por compatibilidade com o schema histórico e não é mais consumida pelo domínio.

## Backup
Backup exporta os dados do caderno para ZIP v1 e importa lotes de forma aditiva/transacional; não cria tabelas próprias. Newsletters e SharedPreferences não são incluídos no backup v1.

Não apagar dados nem usar migração destrutiva para alterações compatíveis. Atualizar este contrato e os testes sempre que entidades/DAOs mudarem.
