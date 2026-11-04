# Capture Flow — estado atual do aplicativo

**Atualizado:** 2026-11-04. Este documento registra o comportamento observado no código; não é uma promessa de release.

## Produto e fonte de verdade
Capture Flow guarda links, textos e imagens compartilhados como referências locais, organizáveis por categoria e com sub-checklists. O app ativo é Kotlin/Android/Jetpack Compose em `android-native/`; `legacy-expo/` é histórico. Banco local Room/SQLite em `captureflow.db`; sem sincronização em nuvem.

## Arquitetura
- Plataforma ativa: Android nativo, Kotlin, Jetpack Compose e Material 3 em `android-native/`; `legacy-expo/` é histórico.
- Organização Feature-first + MVVM, ainda em um único módulo Gradle.
- `MainActivity.kt`: entrada Android, permissões e encaminhamento de intents/Sharesheet.
- `app/navigation/`: shell Compose e composição da biblioteca; `CaptureLibraryCoordinator` coordena features, sem concentrar regras de negócio.
- `app/di/`: composição de dependências.
- `core/`: utilitários compartilhados e UI infrastructure, sem dependência de features.
- `features/captures/`: domínio, contratos, casos de uso, persistência Room e estado das referências/subtarefas.
- `features/imageimport/`: adaptação de Sharesheet/Photo Picker/câmera, metadados, armazenamento privado e OCR.
- `features/categories/`, `search/`, `dashboard/` e `settings/`: estado e apresentação separados por feature.
- `features/fullscreen/`: visualizador e ações por callbacks; não possui ViewModel próprio enquanto não houver estado de negócio independente.
- `features/youtubeplayer/`: player IFrame em WebView, isolado da feature Fullscreen, com ação de retry em falhas.
- `features/newsletters/`: fontes RSS/Atom, parser, fetch limitado, persistência local, atualização manual e salvamento explícito na Biblioteca.
- Direção de dependências: Presentation → Domain/UseCases/contratos; Data implementa repositórios e acessa Room. Domain não importa Android, Compose, Room ou Data. Features não importam implementações internas umas das outras.
- Composables não acessam DAOs, Room ou rede diretamente. A persistência local Room/SQLite é a fonte canônica; não há sincronização em nuvem.
## Tela principal
- Nome do produto na barra superior e launcher: **Capture Flow**.
- Uma tela principal de referências; sem bottom navigation e sem tela separada de tarefas.
- Menu superior: mostrar/ocultar concluídas e escolher tema (seguir sistema, claro, escuro); preferência em SharedPreferences. O controle Ordenar na seção Categorias permite ordenar os cartões por ordem padrão, nome A–Z ou Z–A e as referências por data recente/antiga ou título A–Z/Z–A. Ambas as preferências são persistidas em SharedPreferences.
- Ordem: cartão para selecionar imagem/gerenciar monitoramento, busca com botão limpar, filtro de categorias em duas linhas dentro de um único container com rolagem horizontal compartilhada e biblioteca de cartões por categoria com rolagem vertical. Todas as categorias ficam visíveis na view; o botão **Ver todas as categorias** abre também o modal com a lista completa.
- Categorias padrão: `Ler depois`, `Verificar`, `Salvar` e `Importante`; `Todas` é somente filtro.
- Conclusão é um estado independente da categoria (`isCompleted`). Marcar/desmarcar uma referência como concluída não altera sua categoria.
- Não existe categoria especial `Concluído` nem filtro de categoria com esse nome. O estado concluído é acessado pelo controle de exibição e pelo filtro da view expandida.
- Busca combina título/conteúdo com categoria selecionada.

## Meu progresso
- Dashboard no topo da biblioteca com o título **Meu progresso**.
- O destaque **Concluído** apresenta a quantidade total de referências concluídas (`isCompleted = true`), independentemente da categoria.
- Cada categoria com referências apresenta uma barra horizontal proporcional ao progresso concluído e os números de concluídas e pendentes. Categorias vazias são omitidas.
- A lista mostra cinco categorias inicialmente. Quando há mais categorias, um botão no rodapé do container expande a lista completa; o mesmo botão permite recolher novamente.
- Os números consideram todas as referências persistidas, sem depender da busca, do filtro atual ou da opção global de ocultar concluídos.
- O resumo é derivado dos fluxos de Captures e categorias; Dashboard não mantém cópia persistida do estado.

## Grupos e thumbnails de referência
- A tela principal agrupa referências por categoria em cartões de largura total; cada grupo mostra até três thumbnails pendentes, o progresso `concluídas/total` (ex.: `1/2 itens`) e a ação “Ver todas · quantidade”. Em `Todas`, cartões sem referências pendentes são omitidos; com uma categoria específica selecionada, o cartão continua visível e informa que não há pendentes.
- Ao expandir uma categoria, a view apresenta filtros **Pendentes** e **Concluídas**, com contagens próprias, e mostra somente o estado selecionado em uma grade de três colunas. A view inicia em Pendentes e permite alternar sem alterar a categoria das referências.
- Os dois componentes ativos de referência são `CategoryCard` (cartão de categoria com até três prévias) e `CategoryThumbnail` (thumbnail da grade expandida). Ambos reutilizam `CompletedBadge` para identificar visualmente referências concluídas quando exibidas. Cada thumbnail usa `ContentScale.Crop`, título sobreposto em degradê escuro e placeholder quando não há imagem acessível.
- Estado de OCR pode exibir indicador de processamento/erro.
- O botão/gesto Back fecha a categoria expandida quando nenhum visualizador está por cima.

## Linha de pensamento / anotações
- Cada referência pode ter uma lista ordenada de blocos, cada um com texto de até 10.000 caracteres e uma imagem opcional.
- O botão de balão abre diretamente o formulário de nova anotação, sem tela intermediária de gerenciamento. Toque prolongado em qualquer área do bloco existente, inclusive sobre a imagem, abre seu formulário de edição/remoção. Um ícone de expansão sobre a imagem indica que o toque simples abre a visualização fullscreen proporcional.
- No fullscreen, os blocos aparecem no canto inferior esquerdo em painel que acompanha a altura natural do texto/imagem, limitado a 248 dp; a rolagem vertical só é necessária quando o conteúdo ultrapassa esse limite. O texto não é truncado artificialmente em duas linhas.
- Ao selecionar ou trocar uma imagem, o usuário visualiza e ajusta o recorte (Original, 1:1, 4:5 ou 16:9) antes de comprimir para armazenamento privado com perfil Alto (JPEG quality 50). A UI codifica o recorte em PNG lossless na memória e envia um ByteArray ao compressor, sem arquivo/URI intermediária. A saída é JPEG privada, com limite de 2560 px e normalização de transparência. Falhas exibem o motivo real e preservam o recorte para nova tentativa. Imagens removidas da coleção são limpas do armazenamento privado.
- A coleção é serializada em formato versionado no campo Room captures.notes já existente. Anotações antigas em texto simples são lidas como um único bloco; não há migration destrutiva. Cancelar não persiste alterações.

## Ordenação da biblioteca
- O menu **Ordenar** contém somente opções da seção **Categorias**: ordem padrão/manual, data (mais recentes/mais antigas) e nome (A–Z/Z–A). A data é calculada a partir de `createdAt` das referências de cada categoria: máximo para “Mais recentes” e mínimo para “Mais antigas”. Os filtros de ordenação de referências foram removidos da interface.
- A ordenação por data usa a referência mais recente ou a mais antiga de cada categoria, respectivamente; categorias sem referências correspondentes ficam ao final. Empates preservam a ordem cadastrada.
- Essa preferência altera somente a posição dos containers. A prévia de thumbnails mantém a ordem natural da biblioteca. A grade expandida usa a ordem padrão de referências mais recentes, sem filtro de ordenação configurável.

## Anotações com imagem
- Ao adicionar/trocar uma imagem na anotação, o usuário escolhe o recorte primeiro. O recorte intermediário é codificado como PNG lossless em memória (ByteArray) e então comprimido uma única vez em JPEG com perfil Alto para o armazenamento privado; não há arquivo ou URI intermediária.
- Se a codificação PNG em memória ou a compressão falhar, a imagem/recorte selecionado permanece disponível para nova tentativa. Não há arquivo intermediário para limpar. A anotação só persiste a URI privada após a compressão concluir com sucesso.

## Visualizador fullscreen ativo
- É aberto ao tocar em um thumbnail dentro de uma categoria expandida.
- Quando a URL original é um link YouTube válido (watch, youtu.be, shorts, live ou embed), o fullscreen monta a feature independente youtubeplayer fora da árvore do HorizontalPager, centralizada e em proporção 16:9. A feature usa a YouTube IFrame Player API dentro de WebView acelerada por hardware, sem autoplay e com controles habilitados. O HTML é carregado com loadDataWithBaseURL para fornecer a identidade do app no HTTP Referer, aplica strict-origin-when-cross-origin e configura origin para o mesmo host base. O player reporta estado pronto e códigos de erro; vídeos não incorporáveis ou indisponíveis precisam ser abertos pelo link original no cabeçalho. A thumbnail persistida pelo Image Import continua disponível para cards e outras superfícies.
- A barra superior mostra data/hora local, categoria, botão para abrir a URL original (somente quando `sourceUrl` contém HTTP(S) válido) e fechar. O mesmo atalho de link também aparece na coluna vertical de ações.
- A imagem ocupa a área restante em fundo preto com `ContentScale.Fit`; não recortar nem esticar.
- O título fica sobre a região superior da imagem com gradiente preto forte que esmaece até transparente.
- `HorizontalPager`: swipe-left avança e swipe-right retorna dentro das capturas da mesma categoria filtrada, sem loop nas extremidades.
- A data/categoria/estado de conclusão e a imagem acompanham a página estabilizada. Concluir/reabrir mantém a categoria original. A dica `‹ Deslize para navegar ›` é overlay e não reserva altura.
- Back nativo fecha apenas o visualizador e mantém a categoria expandida por baixo; Back novamente retorna à biblioteca.
- O modal **Editar título** permite alterar o título e, logo abaixo, selecionar a categoria da referência. A alteração de categoria é persistida pelo ViewModel de Captures e atualiza o detalhe ativo.
 
## Player YouTube (feature independente)
- O player é implementado em features/youtubeplayer, separado da feature Fullscreen. A UI do fullscreen apenas extrai o ID da URL ativa e hospeda o componente.
- O WebView fica fora da árvore do HorizontalPager para evitar que transformações/recortes das páginas afetem a superfície de vídeo do Chromium.
- A página HTML usa YouTube IFrame Player API, loadDataWithBaseURL com base URL https://{applicationId}/, meta referrer strict-origin-when-cross-origin, origin correspondente ao base URL, playsinline=1, controles e autoplay desabilitado.
- A feature não falsifica User-Agent, não expõe permissões nativas ao conteúdo e não persiste dados. O JavaScript bridge é limitado a callbacks de prontidão/erro/estado; a instância WebView é pausada/retomada com o lifecycle e destruída ao sair da composição.
- O YouTube documenta erro 153 como ausência de HTTP Referer ou identificação equivalente. Erros de incorporação (ex.: 101/150 e códigos equivalentes) são restrições do vídeo e não podem ser corrigidos pelo app. Em falha, o link original permanece disponível no cabeçalho e a UI oferece retry. O retry ainda precisa ser validado em dispositivo.

## Compartilhamento de link
1. Android envia `ACTION_SEND` com `text/plain`; `CaptureClassifier` identifica URL HTTP(S).
2. ViewModel expõe uma captura pendente e consulta o HTML em coroutine; nenhuma referência de link é persistida ainda.
3. Requisição usa timeout de conexão/leitura de 4 s, segue redirecionamentos e limita a leitura do HTML a 512 KB.
4. Título: primeiro `H1`; fallback `og:title`, `twitter:title`, `title` do documento e host.
5. Imagem: primeira imagem encontrada em `article`, depois `main`, depois `og:image`/metadados Twitter. URLs relativas são resolvidas contra a URL final.
6. Diálogo mostra título, URL, descrição se disponível, indicador de carregamento e escolha obrigatória entre as quatro categorias. Nenhuma categoria vem pré-selecionada; salvar fica desabilitado durante a requisição.
7. **Salvar referência** persiste título, conteúdo, URL de origem e categoria; em seguida tenta baixar a imagem para armazenamento privado e usa-a como capa. Cancelar descarta a captura pendente sem salvar.
8. Se metadados falharem, o link permanece salvável com título fallback. URL de origem já existente é ignorada para evitar duplicação.
9. Texto compartilhado sem URL continua sendo salvo diretamente com deduplicação por conteúdo. Imagem compartilhada segue importação/OCR, não o diálogo de link.

**Limite:** o parser usa regex sobre o HTML recebido e não executa JavaScript. Páginas dinâmicas, autenticadas ou que bloqueiem requisições podem não fornecer H1/imagem. Não é um navegador/DOM completo.

## Múltiplas imagens por referência
- A tabela `capture_media` armazena até 10 imagens locais por referência, com posição, MIME type, URL de origem e indicador de capa.
- A migration 9→10 importa `captures.imageUri` como capa inicial e mantém a coluna legada para compatibilidade com cartões e telas antigas.
- O import de metadados de links coleta até 10 candidatos de imagem. O download aceita imagens HTTP(S) com MIME de imagem, valida que o arquivo pode ser decodificado e limita cada arquivo a 10 MB.
- O fullscreen permite navegar por imagens com controles explícitos e escolher outra capa. A capa selecionada também atualiza `captures.imageUri`.

## Imagens e OCR
- Photo Picker ou compartilhamento fornece URI; o app tenta copiar a imagem para armazenamento privado, mantendo a URI original como fallback em caso de falha.
- A feature Image Compression aplica JPEG quality 70 (Médio) ou 50 (Alto), com dimensão máxima de 2560 px e correção de orientação EXIF. O perfil é aplicado à câmera e ao Photo Picker antes do OCR e persistência. O valor representa qualidade JPEG, não uma redução fixa de bytes.
- OCR usa ML Kit local. Heurísticas avaliam posição, tamanho, caixas delimitadoras, comprimento e sinais aproximados de interface de vídeo/social/notícias/compras/produtividade.
- Para referência de imagem, o título sugerido é persistido; texto OCR bruto não substitui o conteúdo principal. Erro de OCR não apaga a referência.
- Visualizador usa imagem ajustada, sem recorte. Não excluir imagens escolhidas manualmente da galeria.

## Compressão de imagens
- A feature Image Compression aplica JPEG quality 70 (Médio) ou 50 (Alto), limita a maior dimensão a 2560 px e corrige orientação EXIF.
- O perfil selecionado é aplicado à câmera e ao Photo Picker antes do OCR e persistência. O perfil Alto é obrigatório para imagens de notas.
- Os valores representam qualidade JPEG, não uma redução fixa de bytes. A operação é local e não faz upload das imagens.

## Monitoramento e ação de screenshot
- Serviço foreground opcional observa alterações no MediaStore e identifica imagens recentes por nome/caminho de screenshot.
- A notificação abre a Activity com a ação `ACTION_CREATE_TASK` e a URI da imagem; também oferece ação direta **Salvar como tarefa**.
- A intent usa identificador de dados específico por screenshot e concessão de leitura da URI para evitar reaproveitamento incorreto do PendingIntent.
- A Activity encaminha a URI à UI, que abre a confirmação tanto em inicialização fria quanto quando já está em execução.
- A confirmação apresenta prévia da imagem e exige que o usuário escolha uma categoria cadastrada antes de habilitar **Salvar referência**.
- Ao confirmar, a imagem é copiada para o armazenamento privado e criada na categoria escolhida; o arquivo original só é removido após cópia privada bem-sucedida.
- Requer monitoramento ativado e permissões de imagens/notificações aplicáveis à versão Android.

## Monitoramento de screenshots
- Opcional e habilitado pelo usuário, com permissões de imagens e notificações quando aplicável.
- Foreground service observa MediaStore e procura nomes/pastas que parecem screenshots recentes; é best-effort e varia por fabricante/restrições do sistema.
- Não lê pixels de outros apps nem usa Accessibility Service.
- A notificação apresenta a ação como **Salvar referência**. O valor interno do Intent mantém a string histórica `CREATE_SCREENSHOT_TASK` para compatibilidade com notificações já emitidas; o código usa o nome `ACTION_IMPORT_SCREENSHOT`.

## Banco de dados
- `captures`: id, title, content, kind, category, `sourceUrl` opcional, `imageUri` opcional, `notes` opcional (texto legado ou coleção versionada de blocos), createdAt e `isCompleted`. A coluna legada `previousCategory` continua no schema para compatibilidade, mas é limpa e não participa mais das regras de negócio.
- `capture_subtasks`: id, captureId, title, isCompleted e createdAt; índice por captureId.
- `checklist_tasks`: entidade/DAO legado ainda presente, mas sem tela de tarefas independente na navegação atual.
- Versão Room no código: **11**. Migrações recentes: 5→6 adiciona persistência de categorias; 6→7 adicionou historicamente `previousCategory` e a categoria `Concluído`; 7→8 adiciona `notes`; 8→9 restaura a categoria original dos registros que estavam em `Concluído`, limpa `previousCategory` e remove essa categoria da tabela; 9→10 cria `capture_media` e migra `imageUri` legado para a coleção; 10→11 cria `newsletter_sources` e `newsletter_articles`. Concluir/desconcluir altera somente `isCompleted`.

## Newsletters
- A entrada fica no menu superior da Biblioteca; não há bottom navigation.
- Fontes RSS 2.0 e Atom podem ser adicionadas por URL direta do feed ou URL do site; a descoberta usa rel=alternate e caminhos comuns, incluindo /feed para Substack. Fontes são persistidas localmente em `newsletter_sources`; artigos em `newsletter_articles`.
- O parser trata links relativos, GUID/ID, título, datas, resumo, conteúdo e imagens; payload XML limitado a 1 MB e até 100 artigos por leitura.
- A busca de feed usa HTTP(S), bloqueia hosts locais/endereços privados após resolução DNS, limita redirecionamentos a cinco, timeout a seis segundos e resposta a 1 MB; usa ETag/Last-Modified quando disponíveis.
- Atualização manual está disponível para a fonte selecionada ou todas as fontes; a UI mostra a última atualização e o erro da fonte. WorkManager periódico está pendente de definição de frequência. A retenção mantém até 100 artigos não salvos por fonte; artigos vinculados à Biblioteca são preservados.
- O leitor mostra título e resumo/conteúdo disponível no feed; **Abrir original** abre o artigo no navegador. Salvar na Biblioteca cria uma referência em `Ler depois` e vincula o artigo ao ID salvo.
- Não há scraping de páginas autenticadas, bypass de paywall, embed Instagram próprio ou sincronização em nuvem.

## Privacidade e validação
- Dados e imagens permanecem no dispositivo; sem cloud sync.
- Links acionam requisição ao host fornecido pelo usuário; HTML limitado por timeout/tamanho. Download de capa acontece somente após confirmação de categoria.
- Validar em CI o commit exato e executar `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` em `android-native/`.
- Testar Sharesheet (URL, texto+URL, texto simples, imagem), H1/metadata fallbacks, imagem relativa, redirects, timeout/offline, cancelamento, quatro categorias, duplicatas, OCR, permissões, screenshot notification, Back nativo e telas estreitas/fonte ampliada.

## Estado arquitetural — 30/10/2026

A branch `refactor/complete-feature-architecture` aplica os limites de domínio e dados e separa o estado das principais features. O ViewModel de Captures não acessa DAOs nem entidades Room. A importação de links/imagens e OCR foi movida para `ImageImportViewModel`; categorias, pesquisa, dashboard e tema têm ViewModels próprios. A Activity atua como entrada Android, encaminha Sharesheet através de `SharedContentIntentParser` em Image Import e delega a composição visual raiz a `app/navigation`.

A UI foi dividida entre shell de navegação e `CaptureLibraryCoordinator` no App, componentes de Captures, categorias, pesquisa, dashboard e fullscreen. O helper de carregamento de bitmap foi movido para `core/ui`. O fullscreen mantém conclusão, título, anotações, navegação paginada e sub-checklist por `captureId`; as subtarefas são recebidas e alteradas através do `CaptureViewModel`.

A tabela histórica `checklist_tasks` permanece apenas como compatibilidade de schema; as subtarefas ativas são propriedade de Captures e vinculadas por `captureId`. Fullscreen não tem ViewModel independente por não possuir estado de negócio isolado.

Room permanece v11. Não há sincronização cloud e as regras local-first continuam preservadas. A validação final depende de CI verde no HEAD mais recente e revisão manual dos fluxos existentes.


## Atualização — câmera e metadados do YouTube
- A câmera CameraX aceita toque no preview para iniciar foco automático na região selecionada.
- Após fotografar, a pessoa pode manter a imagem original ou aplicar recorte central nos formatos 1:1, 4:5 ou 16:9 antes de iniciar OCR e revisão.
- Para links do YouTube, a extração tenta primeiro o endpoint oficial oEmbed para obter título e thumbnail; em falha, mantém o fallback HTML já existente. O fluxo de confirmação e a URL original permanecem inalterados.


## Atualização — filtros de categoria e fullscreen concluído
- O filtro da biblioteca apresenta todas as categorias em duas linhas roláveis horizontalmente, com rolagem independente por linha e opção **Ver todas as categorias**.
- O fullscreen mostra `CompletedBadge` sobre a imagem quando a referência está concluída.
- Para referências concluídas, a coluna vertical de ações apresenta somente o checkbox de reabertura; as demais ações voltam a aparecer quando a referência fica pendente. O atalho de anotações permanece separado no canto inferior esquerdo.## Linha de pensamento / anotações
- Cada referência pode ter uma lista ordenada de blocos, cada um com texto de até 10.000 caracteres e uma imagem opcional.
- O botão de balão abre o CRUD da coleção. É possível adicionar, editar e excluir blocos; toque prolongado em um bloco na lista ou no fullscreen abre a edição desse bloco.
- No fullscreen, os blocos aparecem no canto inferior esquerdo em área com altura limitada e rolagem vertical; a área comporta até dois blocos visíveis simultaneamente.
- Imagens dos blocos são copiadas para armazenamento privado com compressão Alta (JPEG quality 50). Imagens removidas da coleção são limpas do armazenamento privado.
- A coleção é serializada em formato versionado no campo Room captures.notes já existente. Anotações antigas em texto simples são lidas como um único bloco; não há migration destrutiva. Cancelar não persiste alterações.

## Visualizador fullscreen ativo
- É aberto ao tocar em um thumbnail dentro de uma categoria expandida.
- Quando a URL original é um link YouTube válido, a feature youtubeplayer hospeda o player IFrame em WebView separado do HorizontalPager. O HTML local fornece HTTP Referer por loadDataWithBaseURL e acompanha os eventos de prontidão/erro por uma bridge restrita a callbacks; autoplay permanece desabilitado. O link original continua disponível no cabeçalho; vídeos que não permitem incorporação exigem abertura externa.
- A barra superior mostra data/hora local, categoria, botão para abrir a URL original (somente quando `sourceUrl` contém HTTP(S) válido) e fechar. O mesmo atalho de link também aparece na coluna vertical de ações.
- A imagem ocupa a área restante em fundo preto com `ContentScale.Fit`; não recortar nem esticar.
- O título fica sobre a região superior da imagem com gradiente preto forte que esmaece até transparente.
- `HorizontalPager`: swipe-left avança e swipe-right retorna dentro das capturas da mesma categoria filtrada, sem loop nas extremidades.
- A data/categoria/estado de conclusão e a imagem acompanham a página estabilizada. Concluir/reabrir mantém a categoria original. A dica `‹ Deslize para navegar ›` é overlay e não reserva altura.
- Back nativo fecha apenas o visualizador e mantém a categoria expandida por baixo; Back novamente retorna à biblioteca.
- O modal **Editar título** permite alterar o título e, logo abaixo, selecionar a categoria da referência. A alteração de categoria é persistida pelo ViewModel de Captures e atualiza o detalhe ativo.
 
## Compartilhamento de link
1. Android envia `ACTION_SEND` com `text/plain`; `CaptureClassifier` identifica URL HTTP(S).
2. ViewModel expõe uma captura pendente e consulta o HTML em coroutine; nenhuma referência de link é persistida ainda.
3. Requisição usa timeout de conexão/leitura de 4 s, segue redirecionamentos e limita a leitura do HTML a 512 KB.
4. Título: primeiro `H1`; fallback `og:title`, `twitter:title`, `title` do documento e host.
5. Imagem: primeira imagem encontrada em `article`, depois `main`, depois `og:image`/metadados Twitter. URLs relativas são resolvidas contra a URL final.
6. Diálogo mostra título, URL, descrição se disponível, indicador de carregamento e escolha obrigatória entre as quatro categorias. Nenhuma categoria vem pré-selecionada; salvar fica desabilitado durante a requisição.
7. **Salvar referência** persiste título, conteúdo, URL de origem e categoria; em seguida tenta baixar a imagem para armazenamento privado e usa-a como capa. Cancelar descarta a captura pendente sem salvar.
8. Se metadados falharem, o link permanece salvável com título fallback. URL de origem já existente é ignorada para evitar duplicação.
9. Texto compartilhado sem URL continua sendo salvo diretamente com deduplicação por conteúdo. Imagem compartilhada segue importação/OCR, não o diálogo de link.

**Limite:** o parser usa regex sobre o HTML recebido e não executa JavaScript. Páginas dinâmicas, autenticadas ou que bloqueiem requisições podem não fornecer H1/imagem. Não é um navegador/DOM completo.

## Imagens e OCR
- Photo Picker ou compartilhamento fornece URI; o app tenta copiar a imagem para armazenamento privado, mantendo a URI original como fallback em caso de falha.
- A feature Image Compression aplica JPEG quality 70 (Médio) ou 50 (Alto), com dimensão máxima de 2560 px e correção de orientação EXIF. O perfil é aplicado à câmera e ao Photo Picker antes do OCR e persistência. O valor representa qualidade JPEG, não uma redução fixa de bytes.
- OCR usa ML Kit local. Heurísticas avaliam posição, tamanho, caixas delimitadoras, comprimento e sinais aproximados de interface de vídeo/social/notícias/compras/produtividade.
- Para referência de imagem, o título sugerido é persistido; texto OCR bruto não substitui o conteúdo principal. Erro de OCR não apaga a referência.
- Visualizador usa imagem ajustada, sem recorte. Não excluir imagens escolhidas manualmente da galeria.

## Compressão de imagens
- A feature Image Compression aplica JPEG quality 70 (Médio) ou 50 (Alto), limita a maior dimensão a 2560 px e corrige orientação EXIF.
- O perfil selecionado é aplicado à câmera e ao Photo Picker antes do OCR e persistência. O perfil Alto é obrigatório para imagens de notas.
- Os valores representam qualidade JPEG, não uma redução fixa de bytes. A operação é local e não faz upload das imagens.

## Monitoramento e ação de screenshot
- Serviço foreground opcional observa alterações no MediaStore e identifica imagens recentes por nome/caminho de screenshot.
- A notificação abre a Activity com a ação `ACTION_CREATE_TASK` e a URI da imagem; também oferece ação direta **Salvar como tarefa**.
- A intent usa identificador de dados específico por screenshot e concessão de leitura da URI para evitar reaproveitamento incorreto do PendingIntent.
- A Activity encaminha a URI à UI, que abre a confirmação tanto em inicialização fria quanto quando já está em execução.
- A confirmação apresenta prévia da imagem e exige que o usuário escolha uma categoria cadastrada antes de habilitar **Salvar referência**.
- Ao confirmar, a imagem é copiada para o armazenamento privado e criada na categoria escolhida; o arquivo original só é removido após cópia privada bem-sucedida.
- Requer monitoramento ativado e permissões de imagens/notificações aplicáveis à versão Android.

## Monitoramento de screenshots
- Opcional e habilitado pelo usuário, com permissões de imagens e notificações quando aplicável.
- Foreground service observa MediaStore e procura nomes/pastas que parecem screenshots recentes; é best-effort e varia por fabricante/restrições do sistema.
- Não lê pixels de outros apps nem usa Accessibility Service.
- A notificação apresenta a ação como **Salvar referência**. O valor interno do Intent mantém a string histórica `CREATE_SCREENSHOT_TASK` para compatibilidade com notificações já emitidas; o código usa o nome `ACTION_IMPORT_SCREENSHOT`.

## Banco de dados
- `captures`: id, title, content, kind, category, `sourceUrl` opcional, `imageUri` opcional, `notes` opcional (texto legado ou coleção versionada de blocos), createdAt e `isCompleted`. A coluna legada `previousCategory` continua no schema para compatibilidade, mas é limpa e não participa mais das regras de negócio.
- `capture_subtasks`: id, captureId, title, isCompleted e createdAt; índice por captureId.
- `checklist_tasks`: entidade/DAO legado ainda presente, mas sem tela de tarefas independente na navegação atual.
- Versão Room no código: **9**. Migrações recentes: 5→6 adiciona persistência de categorias; 6→7 adicionou historicamente `previousCategory` e a categoria `Concluído`; 7→8 adiciona `notes`; 8→9 restaura a categoria original dos registros que estavam em `Concluído`, limpa `previousCategory` e remove essa categoria da tabela. Concluir/desconcluir altera somente `isCompleted`.

## Privacidade e validação
- Dados e imagens permanecem no dispositivo; sem cloud sync.
- Links acionam requisição ao host fornecido pelo usuário; HTML limitado por timeout/tamanho. Download de capa acontece somente após confirmação de categoria.
- Validar em CI o commit exato e executar `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` em `android-native/`.
- Testar Sharesheet (URL, texto+URL, texto simples, imagem), H1/metadata fallbacks, imagem relativa, redirects, timeout/offline, cancelamento, quatro categorias, duplicatas, OCR, permissões, screenshot notification, Back nativo e telas estreitas/fonte ampliada.

## Estado arquitetural — 30/10/2026

A branch `refactor/complete-feature-architecture` aplica os limites de domínio e dados e separa o estado das principais features. O ViewModel de Captures não acessa DAOs nem entidades Room. A importação de links/imagens e OCR foi movida para `ImageImportViewModel`; categorias, pesquisa, dashboard e tema têm ViewModels próprios. A Activity atua como entrada Android, encaminha Sharesheet através de `SharedContentIntentParser` em Image Import e delega a composição visual raiz a `app/navigation`.

A UI foi dividida entre shell de navegação e `CaptureLibraryCoordinator` no App, componentes de Captures, categorias, pesquisa, dashboard e fullscreen. O helper de carregamento de bitmap foi movido para `core/ui`. O fullscreen mantém conclusão, título, anotações, navegação paginada e sub-checklist por `captureId`; as subtarefas são recebidas e alteradas através do `CaptureViewModel`.

A tabela histórica `checklist_tasks` permanece apenas como compatibilidade de schema; as subtarefas ativas são propriedade de Captures e vinculadas por `captureId`. Fullscreen não tem ViewModel independente por não possuir estado de negócio isolado.

Room permanece v11. Não há sincronização cloud e as regras local-first continuam preservadas. A validação final depende de CI verde no HEAD mais recente e revisão manual dos fluxos existentes.


## Atualização — câmera e metadados do YouTube
- A câmera CameraX aceita toque no preview para iniciar foco automático na região selecionada.
- Após fotografar, a pessoa pode manter a imagem original ou aplicar recorte central nos formatos 1:1, 4:5 ou 16:9 antes de iniciar OCR e revisão.
- Para links do YouTube, a extração tenta primeiro o endpoint oficial oEmbed para obter título e thumbnail; em falha, mantém o fallback HTML já existente. O fluxo de confirmação e a URL original permanecem inalterados.


## Atualização — filtros de categoria e fullscreen concluído
- O filtro da biblioteca apresenta todas as categorias em duas linhas roláveis horizontalmente, com rolagem independente por linha e opção **Ver todas as categorias**.
- O fullscreen mostra `CompletedBadge` sobre a imagem quando a referência está concluída.
- Para referências concluídas, a coluna vertical de ações apresenta somente o checkbox de reabertura; as demais ações voltam a aparecer quando a referência fica pendente. O atalho de anotações permanece separado no canto inferior esquerdo.
