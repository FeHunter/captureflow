# Contrato atual do banco — Captures

**Banco:** Room, arquivo local `captureflow.db`.  
**Versão Room:** 3.

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

## Migrações
- v1 → v2: adiciona `imageUri` às tarefas.
- v2 → v3: adiciona `description` às tarefas.

Não apagar dados nem usar migração destrutiva para alterações compatíveis. Atualizar este contrato e os testes sempre que entidades/DAOs mudarem.
