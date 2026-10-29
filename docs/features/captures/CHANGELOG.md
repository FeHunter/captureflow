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

## OCR orientado por famílias de UI
- Adicionada pesquisa de apps populares e padrões visuais por categoria em app-popularity-and-ocr-ui-research.md.
- O OCR detecta perfis aproximados de vídeo, social, notícias, comércio e produtividade, ajustando pontuação e faixa vertical de títulos.
- O filtro de ruído dá preferência a sinais fortes de interface/URL/metadata, evitando bloquear palavras genéricas isoladas que podem fazer parte do título.
- A documentação diferencia rankings por MAU, downloads e índices de uso e registra as limitações de comparabilidade de 2024–2026.


## Interface unificada, tags de imagem e tema
- Removida a navegação inferior entre Referências e Checklist; ambas as áreas estão na tela principal.
- Adicionado seletor de categoria para imagens escolhidas no dispositivo.
- Adicionado seletor persistente de tema: sistema, claro e escuro, com paleta azul-marinho/ciano inspirada na referência visual fornecida.


## Tela principal simplificada
- Removida a seção independente Checklist da tela principal; permanece a seleção inicial de imagem.
- Removido o formulário manual para adicionar texto/link; o compartilhamento de conteúdo de outros apps continua disponível.
- Campo de busca ganhou ação X para limpar o texto quando preenchido.
- Versão base atualizada para 0.3.1 e título identifica automaticamente o commit por um sufixo curto gerado no build.


## v0.3.2 — Detecção assistida de screenshots
- Versão atualizada para `0.3.2`, com versionCode 4; o título permanece no formato numérico `v0.0.0`.
- Adicionada detecção best-effort de screenshots recentes via MediaStore enquanto o app está em primeiro plano.
- Adicionado pedido explícito de permissão de leitura de imagens e confirmação para enviar screenshot detectada ao app.
- Mantidos o seletor de imagem do sistema e o compartilhamento Android como alternativas.
