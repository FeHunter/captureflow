# Contrato atual do banco — Captures

**Banco:** Room, arquivo local `captureflow.db`.  
**Versão Room:** 4.

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

Ao excluir uma referência, o ViewModel remove seus sub-itens antes de excluir a referência.

Não apagar dados nem usar migração destrutiva para alterações compatíveis. Atualizar este contrato e os testes sempre que entidades/DAOs mudarem.
