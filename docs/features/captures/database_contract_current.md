# Contrato atual do banco — Captures

**Banco:** Room, arquivo local `captureflow.db`.  
**Versão Room:** 9.

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
- `notes`: anotações associadas à referência (até 10.000 caracteres).

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
- v8 → v9: restaura a categoria original dos registros que estavam em `Concluído` usando `previousCategory` (fallback `Salvar`), limpa a coluna legada e remove a categoria `Concluído` da tabela `categories`.
- v7 → v8: adiciona `notes` às referências.

Ao excluir uma referência, o ViewModel remove seus sub-itens antes de excluir a referência.

A conclusão é independente da categoria: `isCompleted` é o único estado de conclusão. Marcar ou reabrir uma referência não altera `category`. A coluna `previousCategory` permanece apenas por compatibilidade com o schema histórico e não é mais consumida pelo domínio.

Não apagar dados nem usar migração destrutiva para alterações compatíveis. Atualizar este contrato e os testes sempre que entidades/DAOs mudarem.
