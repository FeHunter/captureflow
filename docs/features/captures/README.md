# Captures — Biblioteca

Ownership do agregado Capture: modelos Capture/Category/Subtask/Media/Note, CaptureNotesCodec, contrato CaptureRepository, Room Data, UseCases e CaptureViewModel. CaptureLibraryCoordinator compõe Biblioteca. Notes e Backup usam contratos públicos, sem DAO interno.

Conclusão isCompleted independente de categoria. Categorias padrão Ler depois, Verificar, Salvar, Importante; personalizadas via Categories. Subtarefas em capture_subtasks por captureId. Nova tarefa manual exige título/categoria e cria kind=text sem imagem, URL, OCR ou compressão. checklist_tasks é legado, sem UI autônoma.

Biblioteca: Fotografar/Galeria/Nova tarefa; cards de categoria com prévias pendentes/progresso; grade expandida Pendentes/Concluídas em três colunas. TaskPaperCover para sem imagem; CompletedBadge para concluídas. Settings controla ordenação dos containers e da grade. Até 10 mídias por referência; notas versionadas em captures.notes.

## Edição de capa
O modal Editar título reúne Título principal, Categoria e a ação Alterar imagem da capa via Photo Picker. CaptureViewModel usa ImageCompressor no dispatcher IO e chama CaptureRepository.replaceCoverImage. O repositório atualiza a mídia marcada como capa (ou cria uma se não existir) e sincroniza captures.imageUri em transação. A galeria restante é preservada; arquivo anterior só é removido após sucesso.


## Desempenho de imagens
Cards e prévias de categoria solicitam decode limitado a 360 px por dimensão e usam o cache LRU compartilhado de core/ui; o Fullscreen solicita até 1600 px. O cache é transitório, limitado em memória e indexado por URI/versão do arquivo e dimensão de decode.

## Compartilhamento
A feature Share recebe um snapshot do Capture, mídias e blocos de Notes. Captures continua como fonte de verdade; Share não grava alterações no agregado.
