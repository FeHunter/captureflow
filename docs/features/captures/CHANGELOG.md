# Changelog — Captures

## v0.3.0 — Versão, OCR visual e capas
- Cabeçalho usa `BuildConfig.VERSION_NAME`; versionCode passa para 2 e versionName para 0.3.0.
- OCR retorna texto completo, sugestão de título e descrição.
- Heurística classifica linhas por altura/largura da caixa delimitadora e posição na imagem para priorizar texto visualmente proeminente.
- Cartões de referência exibem a imagem anexada como capa.
- Documentação da feature organizada em arquivos dedicados seguindo o padrão da feature de referência fornecida.
- Validação visual/build ainda pendente.

## Correção OCR — manchetes em múltiplas linhas
- A seleção do título usa uma linha-âncora visualmente proeminente e inclui linhas vizinhas com altura semelhante, mesmo quando uma linha é mais estreita.
- A descrição continua guardando o restante do texto reconhecido, enquanto ruídos comuns de interface são filtrados da seleção do título.
- O carregamento de capas usa leitura direta para URIs file:// e ContentResolver para outros esquemas.
- Adicionados critérios de regressão com a manchete da BBC fornecida como exemplo.

## Cartão minimalista e OCR automático
- Referências com imagem iniciam OCR automaticamente após a cópia e inserção local.
- Cartões de Referências exibem somente título, capa, checkbox e exclusão, com feedback transitório de carregamento/erro.
- O texto complementar do OCR deixa de ser gravado no conteúdo da referência; o título extraído é o payload textual persistido para screenshots.
- O botão manual “Analisar imagem / extrair texto” foi removido dos cartões de Referências.

## OCR YouTube, fullscreen e sub-checklist
- O filtro de ruído do OCR identifica linhas misturadas com domínio do YouTube, “Open App”, controles e metadados de engajamento como likes/views/comments.
- Tocar na capa da referência abre uma visualização fullscreen com a imagem ajustada à tela e ação para fechar.
- Referências aceitam sub-checklists persistentes com inclusão, conclusão e remoção de itens.
- Room migra da versão 3 para 4 criando a tabela `capture_subtasks`; excluir uma referência remove os sub-itens associados.
- Adicionados critérios de teste para a screenshot do YouTube mobile, fullscreen, persistência e exclusão de sub-itens.
