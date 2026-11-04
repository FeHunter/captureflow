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
- `presentation/screens/NewslettersScreen.kt`: interface acessada pelo menu superior.

## Fluxo
1. Usuário informa uma URL HTTP(S) de feed ou de site.
2. O app tenta o feed direto; se necessário, descobre rel=alternate e caminhos comuns, incluindo /feed para Substack.
3. O fetcher valida host público, limita redirects a cinco, timeout a seis segundos e payload a 1 MB.
4. O parser identifica metadados do canal e até 100 artigos.
4. A fonte e os artigos são persistidos no Room; GUID/ID e URL canônica evitam duplicação.
5. Atualização manual por fonte ou em lote usa ETag/Last-Modified quando disponíveis.
6. Artigos excedentes são removidos por retenção, preservando registros já salvos na Biblioteca.
7. Salvar artigo cria uma referência na categoria `Ler depois`; abrir original direciona ao navegador.

## Segurança e limites
- Não aceitar schemes diferentes de HTTP/HTTPS, URLs com credenciais, localhost, domínios .local ou endereços IP privados/reservados detectados na resolução DNS.
- Cada redirecionamento é validado novamente. O cliente não segue redirects automaticamente.
- O corpo do feed é limitado a 1 MB; o parser limita campos e número de itens.
- A validação DNS é best-effort; a conexão HTTP ainda depende da resolução de rede do sistema e não implementa pinning de IP.
- Não executar JavaScript, não raspar páginas autenticadas e não contornar paywall ou restrições de plataformas.
- A UI mostra conteúdo e resumo presentes no feed. O artigo completo é aberto no site de origem quando necessário.
- A remoção da fonte exige confirmação; a navegação de retorno aceita o botão da tela e o Back nativo.
- Imagens de artigos não são baixadas/cacheadas nesta entrega.

## Banco
Room v11: `newsletter_sources` e `newsletter_articles`, com FK em sourceId, exclusão em cascata, índice único por feedUrl e deduplicação por fonte + stableId/URL canônica. Migration 10→11.
