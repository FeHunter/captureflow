# Capture Flow — Planejamento de expansão de conteúdo

> **Status:** implementação em andamento — baseline validado; suporte de persistência multi-mídia iniciado  
> **Data:** 04/11/2026  
> **Repositório:** `FeHunter/captureflow`  
> **Plataforma ativa:** Android nativo, Kotlin, Jetpack Compose, Material 3 e Room  
> **Objetivo:** definir uma evolução segura para referências com múltiplas mídias, melhor suporte a conteúdo incorporado do YouTube e uma área de Newsletters baseada inicialmente em feeds RSS/Atom.

---

## 1. Resumo executivo

Esta atualização amplia o Capture Flow de uma biblioteca de referências predominantemente unitárias para uma experiência que também permite consultar e acompanhar conteúdo editorial.

A proposta está organizada em três frentes independentes, com dependências e critérios próprios:

1. **Mídias múltiplas por referência:** permitir que uma referência originada de um link contenha uma coleção ordenada de imagens, exibida como galeria/slider quando fizer sentido.
2. **Estabilização do player YouTube:** corrigir e validar o player IFrame já existente no aplicativo, com atenção especial ao erro 153, ao HTTP Referer e aos vídeos que não permitem incorporação.
3. **Newsletters:** adicionar uma área separada da Biblioteca para cadastrar publicações e acompanhar artigos por RSS/Atom, com opção explícita de salvar um artigo como referência.

A evolução deve preservar os princípios existentes:

- Android nativo e arquitetura Feature-first + MVVM.
- Persistência local-first com Room.
- Nenhuma sincronização em nuvem implícita.
- Captura e importação sob ação explícita do usuário.
- Falhas de metadados, OCR, feed ou player não podem apagar nem invalidar uma referência já salva.
- A Biblioteca continua sendo o espaço de referências escolhidas pelo usuário; o fluxo automático de artigos não deve misturar-se com ela.
- Nenhuma integração pode contornar autenticação, paywall, privacidade ou restrições das plataformas de origem.

As decisões de produto para a primeira entrega foram aprovadas em 04/11/2026. A implementação seguirá por etapas, com validação de CI após cada grupo coerente de mudanças.

## 2. Diagnóstico do estado atual

A análise técnica anterior identificou os seguintes pontos no código ativo. Eles devem ser reconfirmados no HEAD da branch de implementação antes de qualquer alteração, pois o código pode evoluir entre o planejamento e a execução.

### 2.1 Importação de links e imagens

A feature de importação já recebe conteúdo pelo Android Sharesheet e trata links e imagens em fluxos distintos.

O serviço atual de metadados:

- Faz requisição HTTP(S) com timeout curto e limite de leitura do HTML.
- Tenta obter título por H1 e metadados Open Graph/Twitter, com fallback para o título do documento ou host.
- Procura uma imagem em áreas como article/main e depois em metadados.
- Resolve URLs relativas com base na URL final.
- Possui tratamento específico para oEmbed do YouTube.
- Usa parsing best-effort baseado no HTML recebido; não executa JavaScript e não representa um navegador completo.
- Retorna uma única imagem de capa, em vez de uma coleção ordenada de imagens.

O modelo atual de referência contém um único campo de imagem (`imageUri`). Portanto, exibir um slider real exige evolução de domínio e persistência; não basta alterar somente a UI.

### 2.2 Player YouTube

O projeto já possui uma feature de player incorporado usando WebView e YouTube IFrame Player API. A proposta não é substituir uma implementação inexistente, mas diagnosticar e estabilizar a implementação atual.

Pontos já identificados:

- Player hospedado em WebView Android.
- HTML local carregado com `loadDataWithBaseURL`.
- IFrame Player API com controles habilitados e autoplay desabilitado.
- Proporção 16:9.
- Eventos de prontidão e erro tratados pela feature.
- Link original permanece disponível como alternativa.
- O erro 153 está relacionado à ausência de HTTP Referer ou identificação equivalente válida na requisição do player.
- Erros 101/150/152 podem indicar que o proprietário do vídeo não permite incorporação; não são falhas que o aplicativo deva tentar contornar.

### 2.3 Newsletters

O aplicativo ainda não possui uma área de fontes editoriais, feeds ou artigos recebidos automaticamente. Essa funcionalidade deve ser construída como uma feature independente, sem transformar cada artigo recebido em uma referência automaticamente.

RSS/Atom é a primeira integração recomendada porque permite acompanhar publicações que disponibilizam feeds públicos sem exigir acesso à caixa de e-mail do usuário.

### 2.4 Fonte de verdade e documentação

A documentação de estado atual em `docs/current-state.md` descreve o produto e a arquitetura observados. A documentação de handoff visual está em elaboração no PR #55.

**Atualização da auditoria:** `AGENTS.md` está presente na raiz de `main`. `docs/current-state.md` foi reconciliado com a arquitetura ativa e com a versão Room 9 antes da primeira alteração de schema. O baseline CI no commit `33c65e1` passou em testes unitários, APK Debug e compilação dos testes instrumentados.

---

## 3. Objetivos, não objetivos e limites

### 3.1 Objetivos

- Preservar várias imagens relacionadas ao mesmo link, sem perder a capa atual.
- Apresentar galerias navegáveis quando houver mais de uma imagem válida.
- Melhorar a extração de imagens de fontes compatíveis, especialmente Substack e páginas públicas convencionais.
- Manter uma integração de Instagram dentro dos limites oficiais e técnicos da plataforma.
- Tornar o player YouTube previsível, observável e resiliente.
- Permitir cadastrar, atualizar e remover fontes de Newsletter.
- Ler artigos por RSS/Atom e apresentá-los em uma experiência de leitura própria.
- Permitir salvar manualmente um artigo na Biblioteca.
- Manter dados e mídias no dispositivo, com controles claros de armazenamento e atualização.

### 3.2 Não objetivos da primeira entrega

- Sincronização entre dispositivos ou backend próprio.
- Login obrigatório ou conta Capture Flow.
- Leitura automática da caixa de entrada Gmail/Outlook.
- Extração de conteúdo privado, autenticado ou protegido por paywall.
- Download irrestrito de mídia de Instagram.
- Reprodução de qualquer vídeo independentemente das permissões de incorporação.
- Transformar a Biblioteca em um leitor RSS ou em uma timeline automática.
- Reintroduzir uma tela global de tarefas ou bottom navigation sem decisão de produto e Design.

---

## 4. Frente A — Múltiplas imagens por referência

### 4.1 Comportamento esperado

Ao compartilhar um link compatível:

1. O Capture Flow identifica e normaliza a URL.
2. Um provider de metadados tenta obter título, descrição e imagens disponíveis.
3. A pessoa revisa o título e escolhe uma categoria, como no fluxo atual.
4. A prévia pode apresentar a capa e, quando houver outras imagens válidas, uma galeria navegável.
5. Ao salvar, todas as mídias aprovadas são associadas à mesma referência, preservando ordem e capa.
6. No fullscreen, a pessoa navega pelas imagens da referência sem perder acesso ao título, categoria, link original, conclusão e anotações.

A existência de uma única imagem continua sendo um caso válido. Referências sem imagem também continuam válidas.

### 4.2 Modelo de domínio proposto

Evoluir o modelo de uma única imagem para uma coleção de mídias associadas à referência.

Entidade conceitual proposta:

| Campo | Finalidade |
|---|---|
| `id` | Identificador estável da mídia |
| `captureId` | Referência proprietária |
| `uri` | URI/identificador do arquivo privado local |
| `mediaType` | Tipo de mídia suportado |
| `position` | Posição explícita na coleção |
| `isCover` | Indica a mídia principal da referência |
| `sourceUrl` | URL original da mídia, se necessária para auditoria/reprocessamento |
| `createdAt` | Data de inclusão |

Nome conceitual sugerido: `CaptureMedia`.

A implementação deve decidir se `sourceUrl` é realmente necessária após avaliar privacidade, deduplicação e possibilidade de reprocessamento. Não persistir campos redundantes sem uso definido.

**Compatibilidade:** o campo legado `captures.imageUri` não deve ser removido abruptamente. A migração deve preservar todas as capas existentes e estabelecer uma regra determinística para compatibilidade entre a imagem antiga e a nova coleção. A decisão recomendada é importar `imageUri` como mídia de capa na nova tabela e manter a coluna antiga durante uma janela de compatibilidade, removendo-a somente em uma migração futura deliberada.

### 4.3 Persistência e armazenamento

- Criar tabela filha relacionada a `captures`, com chave estrangeira e índice por `captureId`.
- Definir ordenação explícita e estável por `position`.
- Garantir uma única capa por referência, ou documentar claramente a regra de fallback quando nenhuma mídia estiver marcada.
- Executar migração Room não destrutiva a partir da versão efetivamente existente no momento da implementação.
- Não assumir o número da versão Room indicado por documentação histórica; verificar a versão e as migrações no código atual.
- Copiar mídias aprovadas para armazenamento privado do app, respeitando os limites de compressão e orientação já adotados.
- Limitar dimensões, bytes, quantidade de imagens e formatos aceitos; os valores devem ser definidos com base em testes de memória e desempenho.
- Não confiar apenas na extensão do arquivo. Validar MIME real e capacidade de decodificação.
- Tratar URI temporária, permissão revogada, download parcial, arquivo corrompido e falta de espaço.
- Ao excluir uma referência, limpar somente arquivos privados que pertençam a ela. Nunca apagar arquivos de origem do usuário.

### 4.4 Estratégia de providers de metadados

Evitar acumular exceções por domínio em um único serviço monolítico. Propor uma interface de provider com responsabilidades pequenas, por exemplo:

- `LinkMetadataProvider`: contrato de resolução de metadados.
- `GenericHtmlMetadataProvider`: páginas públicas convencionais.
- `SubstackMetadataProvider`: descoberta/consumo de RSS e metadados públicos da publicação.
- `YouTubeMetadataProvider`: metadados e thumbnail oficial/oEmbed quando aplicável.
- `InstagramLinkProvider`: identificação do tipo de URL e decisão entre embed suportado e fallback.

A seleção deve ser determinística por host e tipo de conteúdo. Providers específicos podem delegar ao provider genérico quando não reconhecerem o formato.

Para HTML convencional, avaliar o uso de parser HTML robusto (por exemplo, Jsoup) em vez de ampliar regex. A dependência só deve ser adicionada após verificar versão, compatibilidade Android, tamanho, manutenção e política de dependências do projeto.

### 4.5 Estratégia por plataforma

**Sites públicos convencionais**
- Priorizar Open Graph, Twitter Card, imagens do artigo e imagens responsivas.
- Resolver URLs relativas e entidades HTML.
- Considerar `srcset`, `picture/source`, lazy loading e atributos comuns de imagem.
- Deduplicar URLs normalizadas.
- Ordenar por contexto do documento, evitando ícones, avatares, anúncios e imagens decorativas quando possível.
- Não executar scripts de páginas remotas.

**Substack**
- Tentar descobrir feed RSS público da publicação, incluindo a convenção `/feed` quando aplicável.
- Processar conteúdo exposto no RSS/Atom e extrair imagens do HTML do item.
- Usar título, URL canônica, data e descrição do feed quando disponíveis.
- Respeitar conteúdo que só esteja disponível para assinantes; não contornar paywall nem autenticação.
- Se o feed não trouxer imagens ou artigo completo, manter a captura como link com capa disponível, sem apresentar a extração como garantida.

**Instagram**
- Não assumir que a página pública fornece o HTML completo: muitos conteúdos dependem de JavaScript, autenticação, restrições anti-bot ou endpoints internos.
- Não construir scraping que contorne login, controles de acesso ou limites da plataforma.
- OEmbed deve ser tratado como mecanismo de incorporação/visualização dentro das condições oficiais, não como garantia de acesso a todas as imagens do post/carrossel.
- Não prometer extração de Stories, posts privados, conteúdo restrito ou todas as imagens de um carrossel.
- Para contas e ativos que o usuário controla e para os quais existam permissões oficiais, uma integração autorizada com APIs Meta pode ser avaliada separadamente.
- Quando não for possível obter mídia legitimamente, salvar o link, título disponível e permitir abrir o conteúdo na plataforma original.

### 4.6 Experiência visual

- Revisão de link: mostrar capa e indicador de quantidade de imagens quando houver galeria.
- Fullscreen: galeria horizontal ou paginação com indicador de posição (ex.: 2 de 5).
- Gestos devem ser previsíveis e não competir com o swipe já usado para navegar entre referências.
- Diferenciar navegação entre imagens da mesma referência e navegação entre referências. Avaliar uma interação aninhada que não crie conflito de gestos; se necessário, preferir controles explícitos de imagem.
- Preservar `Fit` para imagens que precisam ser vistas por inteiro; thumbnails podem usar `Crop`.
- Oferecer fallback visual quando uma mídia falhar, sem quebrar a navegação das demais.
- Definir como a capa é escolhida e como o usuário pode alterá-la. A alteração manual de capa é uma decisão de produto a validar com Design.

### 4.7 Critérios de aceite

- [ ] Uma referência existente com `imageUri` é migrada sem perda.
- [ ] Uma referência pode ter zero, uma ou várias mídias.
- [ ] A ordem é estável após fechar e reabrir o app.
- [ ] A capa é determinística e não muda sem ação/regra explícita.
- [ ] Duplicatas são filtradas sem remover imagens distintas.
- [ ] Uma imagem inválida não impede salvar o link ou as outras imagens válidas.
- [ ] Falha de rede, timeout ou HTML dinâmico mantém o fluxo de captura utilizável.
- [ ] Excluir referência remove arquivos privados associados, sem apagar originais do usuário.
- [ ] O fullscreen permite distinguir navegação da galeria e navegação entre referências.
- [ ] O comportamento é testado em telas pequenas, orientação diferente e memória limitada.

---

## 5. Frente B — Estabilização do player YouTube

### 5.1 Objetivo

Corrigir a experiência do player que já existe, sem substituir a integração oficial por APIs obsoletas ou tentar contornar restrições de vídeos.

### 5.2 Plano de diagnóstico

Antes de alterar a implementação:

1. Reproduzir o erro em dispositivo físico e em emulador, registrando versão Android, versão do Android System WebView, conectividade e versão do app.
2. Coletar logs do WebView, console JavaScript, eventos da IFrame API e ciclo de vida da Activity.
3. Confirmar o `applicationId` real do build instalado e o valor utilizado como identidade de origem.
4. Inspecionar a requisição/documentação aplicável para verificar se o HTTP Referer efetivamente identifica o aplicativo, e não apenas se `loadDataWithBaseURL` foi chamado.
5. Validar consistência entre base URL, `origin` enviado ao player e identidade do app.
6. Confirmar que o player tem dimensão mínima válida e área visível suficiente.
7. Verificar se a WebView não está sendo recriada continuamente por recomposição, paginação ou mudanças de estado.
8. Testar `onResume`, `onPause`, destruição, retorno do fullscreen e mudanças de conectividade.
9. Reproduzir com vídeos públicos que permitem embed e com vídeos que não permitem, para separar falha técnica de restrição do conteúdo.

### 5.3 Tratamento dos códigos de erro

| Sinal | Interpretação | Comportamento esperado |
|---|---|---|
| 153 | Identificação HTTP Referer ausente/inválida ou equivalente não fornecido | Diagnosticar identidade e carregamento; não ocultar o erro |
| 101 / 150 | Vídeo não permite incorporação | Exibir fallback para abrir no YouTube |
| 100 | Vídeo removido, privado ou indisponível | Informar indisponibilidade e manter link original |
| 5 | Falha no player HTML5 | Permitir nova tentativa e fallback externo |
| 2 | Parâmetro/ID inválido | Registrar erro de parsing e fallback |
| Timeout | Player não ficou pronto no tempo esperado | Exibir estado recuperável, sem spinner infinito |

O código final deve mapear os códigos realmente emitidos pela IFrame API e não depender apenas desta tabela conceitual.

### 5.4 Regras de implementação

- Manter YouTube IFrame Player API em WebView Android atualizada pelo sistema.
- Não adotar a antiga YouTube Android Player API, que foi descontinuada.
- Manter autoplay desabilitado.
- Preservar controles oficiais do player; não sobrepor controles próprios por cima da área de reprodução.
- Manter o player em proporção 16:9, respeitando o tamanho mínimo exigido pela plataforma.
- Manter o link original sempre acessível fora do player.
- Restringir navegação da WebView ao conteúdo esperado e não habilitar acesso a arquivos locais sem necessidade.
- Revisar bridge JavaScript para expor somente callbacks necessários.
- Evitar cookies ou permissões de terceiros além do estritamente necessário e documentado.
- Respeitar ciclo de vida e liberar a WebView corretamente.
- Não tentar desbloquear vídeos, ignorar restrições regionais ou contornar a decisão do proprietário do vídeo.

### 5.5 Critérios de aceite

- [ ] Vídeo público com embed permitido carrega em dispositivo físico suportado.
- [ ] Erro 153 não ocorre nos cenários válidos de referência testados.
- [ ] Vídeos que bloqueiam embed exibem fallback claro para abrir o link.
- [ ] Vídeos removidos/privados não deixam estado de carregamento infinito.
- [ ] Player não inicia automaticamente.
- [ ] Controles oficiais permanecem visíveis e utilizáveis.
- [ ] WebView é pausada/retomada/liberada conforme ciclo de vida.
- [ ] Navegar entre referências não deixa áudio ou WebViews antigas em execução.
- [ ] Logs de diagnóstico não registram tokens, cookies ou dados pessoais.
- [ ] O fluxo de imagem/capa e o link original continuam funcionando quando o player falha.

---

## 6. Frente C — Newsletters por RSS/Atom

### 6.1 Conceito de produto

Criar uma área chamada **Newsletters** (nome final sujeito à validação de Design) para acompanhar publicações escolhidas pelo usuário.

A área deve ser independente da Biblioteca:

- **Newsletters:** conteúdo recebido de fontes acompanhadas, com estados de leitura e atualização.
- **Biblioteca:** referências que o usuário decidiu guardar, organizar e acompanhar.

A chegada de um artigo não deve criar automaticamente uma captura na Biblioteca. A ação **Salvar na Biblioteca** é explícita.

### 6.2 Escopo da primeira versão

- Cadastrar uma publicação por URL de site ou URL de feed.
- Descobrir feed RSS/Atom quando possível.
- Permitir inserir diretamente uma URL RSS/Atom.
- Exibir lista de fontes cadastradas.
- Consultar artigos recentes por fonte.
- Atualizar manualmente uma fonte ou todas as fontes.
- Exibir título, publicação/data, resumo e imagem quando disponíveis.
- Marcar artigo como lido/não lido.
- Abrir um leitor interno simples ou abrir o artigo original no navegador, conforme conteúdo disponível.
- Salvar artigo selecionado na Biblioteca.
- Remover fonte e seus artigos locais com confirmação.
- Mostrar estados de carregamento, feed inválido, sem novos artigos, offline e erro recuperável.

### 6.3 RSS/Atom e compatibilidade

- Suportar RSS 2.0 e Atom por parser dedicado.
- Descobrir feeds por links `rel="alternate"` no HTML e por convenções conhecidas quando apropriado.
- Não assumir que toda publicação oferece RSS.
- Deduplicar itens usando GUID/ID e, como fallback, URL canônica normalizada.
- Tratar datas ausentes ou malformadas sem descartar o item.
- Sanitizar HTML antes de renderizar conteúdo de feed.
- Não executar JavaScript de conteúdo remoto.
- Limitar tamanho de resposta, quantidade de itens por atualização e imagens baixadas.
- Preservar a URL original do artigo.
- Tratar feeds que redirecionam, estão inválidos, mudaram de endereço ou exigem autenticação como estados explícitos.

**Substack:** feed público normalmente segue a convenção `https://<publicacao>.substack.com/feed`. A descoberta deve ser tentada, mas a disponibilidade e o nível de conteúdo dependem da publicação.

**Outras plataformas:** Ghost, Medium, blogs e serviços como Beehiiv podem oferecer feeds conforme configuração da publicação. A compatibilidade deve ser validada por feed real; não prometer suporte universal a todas as newsletters.

### 6.4 Modelo de domínio proposto

Entidades conceituais:

**NewsletterSource**
- `id`
- `title`
- `siteUrl`
- `feedUrl`
- `imageUri` opcional
- `lastFetchedAt` opcional
- `lastSyncStatus`
- `createdAt`
- `isEnabled`

**NewsletterArticle**
- `id`
- `sourceId`
- `guid` ou identificador estável
- `title`
- `canonicalUrl`
- `publishedAt` opcional
- `summary` opcional
- `contentHtml` ou conteúdo extraído limitado, conforme estratégia de armazenamento
- `imageUri` opcional
- `isRead`
- `savedCaptureId` opcional
- `fetchedAt`

**NewsletterSyncState** (pode ser tabela ou estado derivado)
- Estado de sincronização da fonte.
- Último sucesso.
- Último erro resumido e seguro para UI.
- Próxima tentativa, se aplicável.

A forma final do schema deve evitar duplicar artigo inteiro quando uma referência já foi criada. Ao salvar artigo na Biblioteca, preservar o vínculo de origem e reutilizar metadados/mídia quando possível, sem acoplar o ciclo de vida das duas features.

### 6.5 Atualização e execução em background

- Atualização manual sempre disponível.
- WorkManager pode executar atualizações periódicas respeitando as restrições do Android; o horário exato não deve ser prometido, pois o sistema pode adiar a execução.
- Definir uma frequência conservadora, com conectividade requerida e backoff em falhas.
- Não manter serviço foreground contínuo para atualizar feeds.
- Não consultar feeds em loop agressivo.
- Respeitar `ETag` e `Last-Modified` quando o servidor oferecer suporte.
- Limitar concorrência, tamanho do feed e quantidade de artigos retidos por fonte.
- Exibir data da última atualização e estado de sincronização.
- Desativar atualização automática por fonte sem remover os artigos já armazenados.

### 6.6 Leitor de artigos

A primeira versão deve priorizar legibilidade e previsibilidade:

- Título, fonte, data e URL original.
- Conteúdo textual disponível no feed, sanitizado.
- Imagens com proporção preservada e carregamento sob demanda.
- Ação para abrir o artigo original.
- Ação para marcar como lido/não lido.
- Ação explícita **Salvar na Biblioteca**.

Quando o feed expuser apenas resumo, o leitor deve deixar isso claro e oferecer abertura do artigo original. Não tentar extrair conteúdo pago ou oculto.

### 6.7 Privacidade e segurança

- RSS/Atom não exige acesso à caixa de e-mail.
- Não solicitar credenciais de publicação.
- Validar esquemas HTTP(S), redirecionamentos e destino final.
- Proteger contra SSRF: bloquear loopback, IPs privados, link-local, endereços internos e esquemas não HTTP(S), inclusive após redirects e resolução DNS.
- Aplicar limites de bytes, tempo, quantidade de redirects e concorrência.
- Sanitizar HTML para prevenir scripts, iframes arbitrários, formulários e conteúdo ativo.
- Não carregar recursos remotos automaticamente sem necessidade; preferir imagens controladas e cacheadas.
- Não armazenar tokens/cookies de terceiros.
- Permitir excluir fontes, artigos e imagens privadas associadas.
- Deixar claro que os feeds são conteúdo de terceiros e podem conter links ou material externo não confiável.

### 6.8 Critérios de aceite

- [ ] É possível cadastrar feed por URL direta.
- [ ] Descoberta automática funciona para páginas que declaram RSS/Atom.
- [ ] RSS 2.0 e Atom válidos são interpretados.
- [ ] GUID/URL evita duplicação entre atualizações.
- [ ] Artigos sem data, imagem ou descrição continuam visíveis.
- [ ] Feed inválido/offline não quebra outras fontes.
- [ ] Atualização manual informa sucesso, falha ou ausência de novos itens.
- [ ] Atualização em background respeita WorkManager e não promete horário exato.
- [ ] Artigos não entram automaticamente na Biblioteca.
- [ ] Salvar na Biblioteca cria uma única referência e preserva a URL de origem.
- [ ] Marcar como lido não altera a conclusão de uma referência.
- [ ] Remover fonte segue política explícita para artigos e capturas já salvas.
- [ ] HTML é sanitizado e destinos de rede são validados.
- [ ] Usuário pode apagar fontes, artigos e mídias locais.

---

## 7. Arquitetura proposta

Manter Feature-first + MVVM e as fronteiras atuais de domínio/dados. A estrutura abaixo é conceitual; os nomes e diretórios finais devem seguir o padrão real já aplicado no repositório.

```text
app/
  navigation/
    ... integração da área Newsletters

features/
  captures/
    domain/
      model/
        Capture
        CaptureMedia
      repository/
    data/
      local/
        CaptureMediaEntity
        CaptureMediaDao
        migrations/
    presentation/
      ... galeria e seleção de capa

  imageimport/
    domain/
      model/
        LinkMetadata
        LinkMedia
      repository/
        LinkMetadataProvider
    data/
      providers/
        GenericHtmlMetadataProvider
        SubstackMetadataProvider
        YouTubeMetadataProvider
        InstagramLinkProvider
      parser/
        HtmlMetadataParser
        FeedDiscoveryParser
    presentation/
      ... revisão de link e galeria

  youtubeplayer/
    domain/
      ... contratos/estado se necessários
    presentation/
      ... WebView/IFrame player e estados

  newsletters/
    domain/
      model/
        NewsletterSource
        NewsletterArticle
        NewsletterSyncState
      repository/
        NewsletterRepository
    data/
      local/
        NewsletterSourceEntity
        NewsletterArticleEntity
        NewsletterDao
    data/
      remote/
        RssAtomFeedParser
        FeedDiscoveryService
        NewsletterSyncWorker
    presentation/
      view/
      viewmodel/

core/
  network/
    ... cliente HTTP compartilhado e políticas de segurança
  storage/
    ... armazenamento privado de mídia, se a abstração for compartilhada
  ui/
    ... componentes reutilizáveis
```

### 7.1 Regras de dependência

- UI/Compose não acessa Room DAO diretamente.
- ViewModels dependem de casos de uso/repositórios, conforme padrão vigente.
- Entidades Room não vazam para o domínio ou UI.
- Providers de metadados não conhecem Composables nem ViewModels.
- Parser RSS/Atom é independente de UI.
- WorkManager chama caso de uso/repositório de sincronização; não contém regras de negócio duplicadas.
- Player YouTube permanece isolado e não controla navegação de Captures.
- Armazenamento privado é acessado por abstração que define ownership e limpeza de arquivos.
- Não criar abstração genérica prematura: compartilhar apenas componentes com mais de um consumidor real.

### 7.2 Dependências externas

Antes de adicionar bibliotecas:

1. Verificar se já existe dependência equivalente no projeto.
2. Confirmar manutenção, licença, compatibilidade com minSdk/targetSdk e tamanho.
3. Preferir bibliotecas pequenas e maduras para parsing RSS/Atom e HTML.
4. Não adicionar SDKs de redes sociais apenas para contornar limitações de scraping.
5. Documentar a razão da dependência e cobrir comportamento com testes.

---

## 8. Navegação e impacto no Design

O handoff atual de UI/UX descreve o aplicativo existente, não estas novas funcionalidades. O Design deve receber uma extensão do escopo antes de implementação visual.

### 8.1 Nova área Newsletters

A decisão de navegação ainda precisa ser validada. A recomendação inicial é adicionar um ponto de entrada claro na top bar/menu ou outra navegação leve, sem introduzir bottom navigation automaticamente.

Telas/superfícies a desenhar:

- Lista de fontes cadastradas.
- Estado vazio com ação para adicionar publicação.
- Fluxo de adicionar fonte por URL.
- Resultado de descoberta de feed e confirmação.
- Lista de artigos de uma fonte.
- Estados não lidos/lidos.
- Leitor de artigo.
- Estado de atualização e erro.
- Gerenciamento/remover fonte.
- Ação salvar artigo na Biblioteca e feedback de sucesso.

### 8.2 Galeria de imagens

- Revisão de link com múltiplas imagens.
- Indicador de quantidade e prévia.
- Fullscreen com navegação entre mídias.
- Indicador de posição.
- Seleção de capa, se aprovada.
- Estado de mídia indisponível.
- Acessibilidade para gestos e controles.

### 8.3 Player YouTube

- Player 16:9.
- Estado de carregamento.
- Estado pronto.
- Erro técnico.
- Vídeo indisponível/não incorporável.
- Ação externa sempre acessível.
- Nenhum overlay cobrindo controles oficiais.

### 8.4 Preservação do produto existente

Não alterar os contratos documentados no handoff atual: categorias padrão, conclusão independente, subtarefas vinculadas à referência, dados local-first e ausência de uma tela independente de tarefas.

---

## 9. Sequência de execução recomendada

A ordem reduz risco e permite validar cada parte sem bloquear as demais.

### Etapa 0 — Preparação e baseline

- [ ] Confirmar branch/HEAD e ler instruções reais do projeto (incluindo localizar ou restaurar `AGENTS.md`, se aplicável).
- [ ] Comparar `docs/current-state.md` com o código ativo e corrigir divergências antes de novas migrações.
- [ ] Confirmar versão Room, schema exportado e migrações existentes.
- [ ] Executar testes unitários e build Android no baseline.
- [ ] Registrar limitações conhecidas e vídeos/feeds de teste públicos.
- [ ] Obter aprovação do Design para os novos fluxos antes de implementar telas.

**Saída:** baseline verde e decisões pendentes registradas.

### Etapa 1 — Diagnóstico e correção do YouTube

- [ ] Reproduzir e classificar os erros.
- [ ] Validar Referer, origin, applicationId e WebView.
- [ ] Corrigir lifecycle/recomposição se necessário.
- [ ] Implementar estados de falha e fallback.
- [ ] Criar testes de parsing de URL e testes manuais de player.
- [ ] Atualizar documentação de troubleshooting.

**Saída:** player estável para vídeos incorporáveis, com fallback claro nos demais casos.

### Etapa 2 — Modelo multi-mídia e migração

- [ ] Definir contrato final de `CaptureMedia`.
- [ ] Criar entidade/DAO/repository e migration não destrutiva.
- [ ] Migrar `imageUri` legado como capa.
- [ ] Atualizar criação, edição e exclusão de referências.
- [ ] Atualizar limpeza de arquivos privados.
- [ ] Testar upgrade de banco real e banco vazio.

**Saída:** domínio e persistência suportam zero, uma ou várias imagens sem regressão.

### Etapa 3 — Providers e extração de imagens

- [ ] Criar contrato de provider.
- [ ] Implementar parser HTML robusto ou justificar alternativa.
- [ ] Implementar provider genérico com limites de segurança.
- [ ] Implementar suporte público RSS/HTML para Substack.
- [ ] Implementar decisão de Instagram (embed/fallback/API autorizada futura).
- [ ] Validar MIME, tamanho, dimensões, deduplicação e ordenação.
- [ ] Cobrir timeout, redirect, HTML malformado e imagens inacessíveis.

**Saída:** importação multi-imagem previsível e com degradação segura.

### Etapa 4 — UI de galeria

- [ ] Atualizar revisão de link.
- [ ] Implementar navegação entre imagens da mesma referência.
- [ ] Resolver conflito de gestos com navegação entre referências.
- [ ] Exibir indicador de posição e capa.
- [ ] Validar acessibilidade, tela pequena e rotação.

**Saída:** galeria funcional integrada ao fluxo atual.

### Etapa 5 — Newsletters RSS/Atom

- [ ] Definir modelo e schema.
- [ ] Implementar descoberta de feed.
- [ ] Implementar parsers RSS 2.0 e Atom.
- [ ] Implementar persistência, deduplicação e atualização manual.
- [ ] Implementar WorkManager com política conservadora.
- [ ] Implementar gerenciamento de fontes e lista de artigos.
- [ ] Implementar leitor e ação salvar na Biblioteca.
- [ ] Implementar exclusão e política de retenção.
- [ ] Testar segurança de rede e sanitização HTML.

**Saída:** acompanhamento de fontes públicas com controle explícito do usuário.

### Etapa 6 — Integração, regressão e documentação

- [ ] Executar toda a suíte unitária.
- [ ] Executar build Debug e, quando aplicável, Release.
- [ ] Testar migrações a partir da versão anterior.
- [ ] Testar fluxos antigos de Sharesheet, câmera, OCR, screenshot, notas, categorias e fullscreen.
- [ ] Atualizar `docs/current-state.md`.
- [ ] Atualizar o handoff de Design com os componentes aprovados.
- [ ] Atualizar instruções do projeto e diagramas de arquitetura.
- [ ] Validar CI no commit exato do PR.

**Saída:** funcionalidades integradas, documentadas e sem regressões conhecidas.

---

## 10. Estratégia de testes

### 10.1 Unitários

- Normalização e validação de URLs.
- Seleção de provider por host.
- Parsing de metadados Open Graph/Twitter/H1.
- Extração e ordenação de imagens.
- Deduplicação de mídias.
- Validação MIME e limites de tamanho.
- Parsing RSS 2.0 e Atom.
- GUID/URL e deduplicação de artigos.
- Datas ausentes/inválidas.
- Transições de estado lido/não lido.
- Conversão de artigo em referência.
- Mapeamento de códigos de erro YouTube.

### 10.2 Integração

- Migration Room com dados reais legados.
- DAO de CaptureMedia e Newsletter.
- Persistência de ordenação/capa.
- Exclusão e limpeza de arquivos privados.
- Sync worker com sucesso, timeout, offline, backoff e resposta 304.
- Feed inválido sem afetar outras fontes.
- Salvamento de artigo sem duplicar referência.

### 10.3 Testes manuais em dispositivo

- Android Sharesheet com URL, texto + URL, imagem e múltiplas imagens.
- Instagram público, privado/restrito e URL que não oferece metadados.
- Substack com feed e artigo com várias imagens.
- YouTube com vídeo incorporável, não incorporável, privado/removido e offline.
- Newsletters com RSS, Atom, redirect, feed vazio e feed malformado.
- Navegação Back e ciclo de vida.
- Modo claro/escuro, fonte ampliada e TalkBack.
- Tela estreita e orientação.
- Pouco espaço em disco e memória limitada.

Não usar conteúdo privado de terceiros como fixture de teste. Preferir HTML/feed sintético versionado e URLs públicas estáveis.

---

## 11. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Instagram não expõe imagens no HTML | Galeria incompleta | Embed oficial/fallback; não prometer scraping |
| Mudança de HTML dos sites | Extração instável | Providers isolados, testes com fixtures e fallback |
| Mídias grandes | Memória/armazenamento | Limites de bytes/dimensões, compressão e carregamento sob demanda |
| Migration multi-mídia | Perda de capa | Migration não destrutiva, backup/fixtures e teste de upgrade |
| Conflito entre gestos | Navegação confusa | Definir prioridade e testar interação aninhada |
| Erro 153 no YouTube | Player não carrega | Diagnosticar Referer/origin real e fallback |
| Vídeo bloqueia embed | Reprodução impossível no app | Abrir link original sem contornar restrição |
| RSS inconsistente | Artigos ausentes/duplicados | Parser robusto, dedupe por GUID/URL e estado de erro |
| Feed malicioso | Execução de conteúdo/SSRF | Sanitização HTML, allowlist de esquema e bloqueio de IPs internos |
| Atualização em background adiada | Conteúdo desatualizado | Mostrar última atualização e oferecer refresh manual |
| Mistura de Newsletters e Biblioteca | Perda de clareza do produto | Features e fluxos separados; salvar é ação explícita |

---

## 12. Decisões aprovadas para a primeira entrega

As decisões abaixo foram confirmadas pelo usuário em 04/11/2026 e passam a orientar a implementação:

1. **Galeria:** máximo de 10 imagens por referência e 10 MB por imagem, com validação de conteúdo real e dimensões.
2. **Capa:** primeira imagem válida é a capa inicial; permitir alteração manual da capa.
3. **Gestos:** controles explícitos/setas para navegar imagens dentro da referência, preservando o swipe horizontal existente entre referências.
4. **Instagram:** embed oficial quando disponível; caso contrário, manter link/fallback. Sem scraping ou bypass de login/paywall.
5. **Newsletters:** nome final `Newsletters`, com ponto de entrada no menu superior; sem bottom navigation.
6. **Leitor:** priorizar o conteúdo presente no feed; quando houver apenas resumo, indicar isso e oferecer abertura do artigo original.
7. **Retenção:** manter até 100 artigos por fonte; ao exceder, remover os mais antigos não salvos, preservando os artigos vinculados a referências da Biblioteca.
8. **Atualização:** manual nesta primeira entrega; atualização automática periódica fica fora do escopo até definir frequência e consumo de rede.
9. **Remoção:** excluir a fonte remove seus artigos e metadados locais em cascata; referências já salvas na Biblioteca permanecem independentes.
10. **Armazenamento:** imagens de artigos/newsletters não são baixadas para cache local nesta entrega; thumbnails permanecem URLs remotas. A limpeza de arquivos privados da galeria de Captures segue a política da feature de armazenamento.

Essas três decisões refletem o comportamento adotado na primeira implementação e podem ser revistas antes de uma futura sincronização automática.

---

## 13. Definição de pronto da atualização

A atualização completa só deve ser considerada pronta quando:

- O player YouTube tiver diagnóstico reproduzível, comportamento correto e fallback.
- O modelo multi-mídia estiver migrado sem perda de dados.
- Links compatíveis puderem fornecer várias imagens e a UI apresentar uma galeria utilizável.
- Limitações do Instagram estiverem refletidas no comportamento e na comunicação da UI.
- Newsletters RSS/Atom puderem ser cadastradas, atualizadas, lidas e removidas.
- Salvar artigo na Biblioteca for explícito e não duplicar conteúdo.
- Segurança de rede, sanitização, armazenamento e exclusão estiverem testados.
- CI estiver verde no HEAD exato do PR.
- Documentação técnica, estado atual, arquitetura e handoff de Design estiverem atualizados.
- Os fluxos existentes de captura, OCR, screenshot, categorias, fullscreen e anotações continuarem funcionando.

---

## 14. Referências técnicas

Fontes oficiais e documentação a consultar novamente durante a implementação:

- YouTube IFrame Player API: https://developers.google.com/youtube/iframe_api_reference
- YouTube Required Minimum Functionality: https://developers.google.com/youtube/terms/required-minimum-functionality
- YouTube Android embedding guidance: https://developers.google.com/youtube/android
- YouTube API Services Terms: https://developers.google.com/youtube/terms/api-services-terms-of-service
- Substack RSS feeds: https://support.substack.com/hc/en-us/articles/360037832191-How-do-I-find-the-RSS-feed-for-a-Substack-publication
- Meta oEmbed documentation: https://developers.facebook.com/docs/instagram/oembed/
- Android WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
- Android WebView: https://developer.android.com/develop/ui/views/layout/webapps/webview
- RSS 2.0 Specification: https://www.rssboard.org/rss-specification
- Atom Syndication Format (RFC 4287): https://www.rfc-editor.org/rfc/rfc4287

As referências são pontos de partida. Confirmar versão, requisitos e políticas vigentes no momento da implementação.

---

## 15. Histórico

- **04/11/2026:** planejamento consolidado a partir da auditoria da importação de links/imagens, da feature YouTube Player e da pesquisa de viabilidade de RSS/Atom e integrações de conteúdo.
- **04/11/2026:** implementação iniciada na branch do PR #56; persistência multi-mídia, importação de imagens, galeria fullscreen, parser RSS/Atom, fetch seguro, persistência de fontes/artigos e tela Newsletters foram adicionados em grupos coerentes.


## 16. Registro de execução — 04/11/2026

### Etapa 0 — concluída
- [x] Confirmada a branch do PR #56 e a versão Room 9 no código.
- [x] `AGENTS.md` localizado na raiz; não foi necessário restaurá-lo.
- [x] `docs/current-state.md` reconciliado com a arquitetura ativa e a versão Room.
- [x] Baseline CI verde: testes unitários, APK Debug e compilação dos testes instrumentados.
- [x] Decisões de produto aprovadas e registradas na seção 12.

### Etapa 2 — múltiplas imagens
- [x] Modelo `CaptureMedia`, entidade, DAO, contrato e implementação do repositório.
- [x] Migration Room 9→10 cria `capture_media` e importa `captures.imageUri` como capa inicial, mantendo a coluna legada.
- [x] Extração de até 10 URLs de imagem dos metadados da página e download de até 10 MB por arquivo.
- [x] Validação do MIME de imagem e decodificação do arquivo antes de persistir.
- [x] Galeria fullscreen com controles explícitos e ação **Definir capa**; a capa atualiza também `captures.imageUri`.
- [ ] Teste de migration com banco v9 contendo imagens reais e teste de exclusão/limpeza de arquivos.
- [ ] Mostrar contagem/prévia das imagens no diálogo de revisão de link.

### Etapa 3 — Newsletters RSS/Atom
- [x] Modelos de domínio, entidades Room, DAO, repositório e migration 10→11.
- [x] Parser RSS 2.0/Atom com suporte a IDs/GUIDs, links relativos, datas, resumo, conteúdo e imagem.
- [x] Fetch HTTP com limite de tamanho, timeout, redirecionamentos limitados, validação de host público e ETag/Last-Modified.
- [x] Cadastro de fonte, atualização manual, deduplicação e retenção de 100 artigos não salvos por fonte.
- [x] Tela Newsletters no menu superior, seleção de fontes, abertura do original, marcação lido/não lido e ação explícita para salvar na Biblioteca.
- [ ] Testes instrumentados executados em dispositivo/emulador (os testes atuais foram adicionados ao source set, mas o workflow só compila androidTest).
- [x] Adicionar confirmação antes de excluir uma fonte; erros de feed inválido e de rede são apresentados como mensagem na tela.

### YouTube e Instagram
- [x] Auditoria do player IFrame/WebView atual e preservação do caminho `loadDataWithBaseURL`/origin; testes unitários do HTML e parser já existem.
- [ ] Validar erro 153, reprodução, pause/resume e destruição da WebView em dispositivo/emulador real.
- [x] Adicionar retry explícito quando o player falhar.
- [x] Instagram permanece no link/fallback externo; nenhum scraping ou contorno de restrições é realizado.

A implementação ainda não está pronta para merge: validar migrations e testes instrumentados em dispositivo, concluir os itens pendentes e aguardar CI verde no HEAD exato.
