# Newsletters

Suporta RSS 2.0/Atom, cadastro por URL de feed/site e descoberta rel=alternate/caminhos comuns. Fetch HTTP(S) valida host, redirects, timeout e payload; parser limita itens/campos. Room v11: newsletter_sources/newsletter_articles. Atualização manual de fonte ou todas; ETag/Last-Modified quando disponível. Retenção preserva artigos salvos. Leitor prioriza conteúdo do feed, pode abrir URL canônica quando há só resumo; Abrir original disponível. Salvar cria referência em Ler depois e vincula artigo. Sem scraping privado, bypass paywall, cache de imagens ou atualização periódica automática. WebView sem JS/DOM Storage/file/mixed content.

## Prévia na Home

A Biblioteca apresenta as três publicações mais recentes da fonte selecionada entre Dashboard e Categorias. A seção observa os StateFlows existentes do NewslettersViewModel e lê artigos já persistidos no Room; não inicia fetch de rede. Tocar em uma publicação abre o leitor existente e marca o artigo como lido. Ver todas abre a tela completa de Newsletters. Sem fonte/artigos, a seção mostra um estado vazio com acesso para adicionar ou atualizar feeds.

A seleção da fonte continua sendo estado em memória do NewslettersViewModel; não foi adicionada persistência de preferência nesta alteração.

## Backup

Newsletters participa do Backup ZIP v2. Fontes e artigos são exportados, incluindo estado isRead e savedCaptureId. Na restauração, a fonte é conciliada por feedUrl; registros locais prevalecem. Artigos são inseridos sem sobrescrever duplicatas. Backup v1 sem campos Newsletter continua suportado.
