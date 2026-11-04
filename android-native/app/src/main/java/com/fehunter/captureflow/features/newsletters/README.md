# Newsletters

Feature para cadastrar e acompanhar publicações por feeds RSS 2.0 e Atom, sem acesso a contas de e-mail ou scraping de páginas privadas.

## Organização
- `domain/model`: `NewsletterSource`, `NewsletterArticle`, `NewsletterArticleDraft` e `ParsedNewsletterFeed`.
- `domain/repository`: contrato `NewsletterRepository`.
- `domain/usecase`: operações de fontes e artigos.
- `data/feed/RssAtomFeedParser.kt`: parser XML RSS 2.0/Atom.
- `data/feed/NewsletterFeedFetcher.kt`: cliente HTTP limitado e validação de URL.
- `data/local`: entidades e DAO Room.
- `data/repository/RoomNewsletterRepository.kt`: implementação de persistência.
- `presentation/viewmodel`: cadastro, atualização, leitura e salvamento na Biblioteca.
- `presentation/screens/NewslettersScreen.kt`: tela de acompanhamento integrada ao shell principal.

## Apresentação e experiência visual
- Segue os tokens semânticos Material 3 do `CaptureFlowTheme`, incluindo superfícies, containers, tipografia e contraste para temas claro/escuro.
- Usa o `CaptureFlowBackdrop` compartilhado, cards com cantos arredondados, hierarquia tipográfica e ações em tonalidade consistente com a Biblioteca.
- A tela organiza o fluxo em cabeçalho, cadastro de fonte, fontes cadastradas, estado da fonte selecionada e lista de publicações.
- Estados vazios, sincronização, erro de atualização e feedback de ações são apresentados explicitamente.
- Cards de artigo destacam leitura/não leitura, data disponível, leitura dentro do app, alternância de leitura e salvamento na Biblioteca.
- Fontes cadastradas aparecem em cartões horizontais com estado selecionado, domínio do site e indicação de erro de sincronização.
- A apresentação não altera contratos de persistência, limites de rede ou regras de negócio.

## Fluxo
1. Usuário informa uma URL HTTP(S) de feed ou de site.
2. O app tenta o feed direto; se necessário, descobre rel=alternate e caminhos comuns, incluindo /feed para Substack.
3. O fetcher valida host público, limita redirects a cinco, timeout a seis segundos e payload a 1 MB.
4. O parser identifica metadados do canal e até 100 artigos.
5. A fonte e os artigos são persistidos no Room; GUID/ID e URL canônica evitam duplicação.
6. Atualização manual por fonte ou em lote usa ETag/Last-Modified quando disponíveis.
7. Artigos excedentes são removidos por retenção, preservando registros já salvos na Biblioteca.
8. Abrir artigo entra no leitor interno: renderiza o HTML completo fornecido pelo feed ou carrega a página canônica em uma WebView quando o feed oferece apenas resumo. Salvar artigo cria uma referência na categoria `Ler depois`.

## Segurança e limites
- Não aceitar schemes diferentes de HTTP/HTTPS, URLs com credenciais, localhost, domínios .local ou endereços IP privados/reservados detectados na resolução DNS.
- Cada redirecionamento é validado novamente. O cliente não segue redirects automaticamente.
- O corpo do feed é limitado a 1 MB; o parser limita campos e número de itens.
- A validação DNS é best-effort; a conexão HTTP ainda depende da resolução de rede do sistema e não implementa pinning de IP.
- Não executar JavaScript, não raspar páginas autenticadas e não contornar paywall ou restrições de plataformas.
- O leitor interno prioriza `contentHtml` armazenado no feed. Quando esse campo não existe, carrega a URL canônica em WebView, sem habilitar JavaScript, acesso a arquivos ou conteúdo misto. Sites que dependem de JavaScript, autenticação ou bloqueiam WebView podem não funcionar integralmente.
- A remoção da fonte exige confirmação; a navegação de retorno aceita o botão da tela e o Back nativo.
- Imagens de artigos não são baixadas/cacheadas nesta entrega.

## Banco
Room v11: `newsletter_sources` e `newsletter_articles`, com FK em sourceId, exclusão em cascata, índice único por feedUrl e deduplicação por fonte + stableId/URL canônica. Migration 10→11.
