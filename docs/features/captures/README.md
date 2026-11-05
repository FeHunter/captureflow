# Captures — Biblioteca

Ownership do agregado Capture: modelos Capture/Category/Subtask/Media/Note, CaptureNotesCodec, contrato CaptureRepository, Room Data, UseCases e CaptureViewModel. CaptureLibraryCoordinator compõe Biblioteca. Notes e Backup usam contratos públicos, sem DAO interno.

Conclusão isCompleted independente de categoria. Categorias padrão Ler depois, Verificar, Salvar, Importante; personalizadas via Categories. Subtarefas em capture_subtasks por captureId. Nova tarefa manual exige título/categoria e cria kind=text sem imagem, URL, OCR ou compressão. checklist_tasks é legado, sem UI autônoma.

Biblioteca: Fotografar/Galeria/Nova tarefa; cards de categoria com prévias pendentes/progresso; grade expandida Pendentes/Concluídas em três colunas. TaskPaperCover para sem imagem; CompletedBadge para concluídas. Settings controla ordenação dos containers e da grade. Até 10 mídias por referência; notas versionadas em captures.notes.