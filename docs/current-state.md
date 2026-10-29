# Capture Flow — estado atual do aplicativo

**Atualizado:** 2026-10-29. Este documento registra o comportamento observado no código; não é uma promessa de release.

## Produto e fonte de verdade
Capture Flow guarda links, textos e imagens compartilhados como referências locais, organizáveis por categoria e com sub-checklists. O app ativo é Kotlin/Android/Jetpack Compose em `android-native/`; `legacy-expo/` é histórico. Banco local Room/SQLite em `captureflow.db`; sem sincronização em nuvem.

## Arquitetura
- `MainActivity.kt`: intents do launcher, Android Sharesheet e notificação.
- `ui/CaptureFlowApp.kt`: tela única, cartões, busca, filtros, diálogos, visualizadores fullscreen e sub-checklist.
- `ui/CaptureViewModel.kt`: StateFlow, operações assíncronas, captura pendente de link, persistência, OCR e subtarefas.
- `data/CaptureDatabase.kt`: entidades, DAOs, classificação e migrações Room.
- `services/LinkMetadataService.kt`: requisição HTTP(S) e parsing best-effort de HTML.
- `services/OcrService.kt`: ML Kit Text Recognition no dispositivo e heurísticas de título.
- `services/SharedImageStorageService.kt`: cópia/download de imagem para diretório privado e remoção segura de arquivos próprios.
- `ScreenshotMonitor.kt` e `ScreenshotNotificationService.kt`: detecção opcional de screenshots pelo MediaStore.
- Direção de dependências: UI → ViewModel → serviços/DAOs. Composables não devem acessar DAOs diretamente.

## Tela principal
- Nome do produto na barra superior e launcher: **Capture Flow**.
- Uma tela principal de referências; sem bottom navigation e sem tela separada de tarefas.
- Menu superior: mostrar/ocultar concluídas e escolher tema (seguir sistema, claro, escuro); preferência em SharedPreferences.
- Ordem: cartão para selecionar imagem/gerenciar monitoramento, busca com botão limpar, chips horizontais e biblioteca de cartões em duas colunas com rolagem vertical.
- Categorias atuais: `Ler depois`, `Verificar`, `Salvar`, `Importante`; `Todas` é somente filtro.
- Busca combina título/conteúdo com categoria selecionada.

## Grupos e thumbnails de referência
- A tela principal agrupa referências por categoria em cartões de largura total; cada grupo mostra até três thumbnails e a ação “Ver todas”.
- Ao expandir uma categoria, as referências aparecem em grade de três colunas. Cada thumbnail usa `ContentScale.Crop`, título sobreposto em degradê escuro e placeholder quando não há imagem acessível.
- Estado de OCR pode exibir indicador de processamento/erro.
- O botão/gesto Back fecha a categoria expandida quando nenhum visualizador está por cima.

## Visualizador fullscreen ativo
- É aberto ao tocar em um thumbnail dentro de uma categoria expandida.
- A barra superior mostra data/hora local, categoria, botão para abrir a URL original (somente quando `sourceUrl` contém HTTP(S) válido), checkbox de conclusão, excluir e fechar.
- A imagem ocupa a área restante em fundo preto com `ContentScale.Fit`; não recortar nem esticar.
- O título fica sobre a região superior da imagem com gradiente preto forte que esmaece até transparente.
- `HorizontalPager`: swipe-left avança e swipe-right retorna dentro das capturas da mesma categoria filtrada, sem loop nas extremidades.
- A data/categoria/estado de conclusão e a imagem acompanham a página estabilizada. A dica `‹ Deslize para navegar ›` é overlay e não reserva altura.
- Back nativo fecha apenas o visualizador e mantém a categoria expandida por baixo; Back novamente retorna à biblioteca.
- Observação: existe um composable `CaptureCard` com outro layout fullscreen e edição de título no mesmo arquivo, mas ele não é referenciado pelo fluxo ativo atual. Não o tratar como funcionalidade visível até reativação explícita.
 
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



## Correção: título OCR e origem real da captura
O OCR continua ativo para ler a imagem e gerar um título útil, mas o texto reconhecido não deve ser tratado automaticamente como URL original. A barra de endereço do navegador, mensagens como “Not secure”, IPs locais, portas e caminhos podem aparecer no OCR sem representar o conteúdo/post capturado. Por isso:
- Nunca preencher `sourceUrl` apenas porque uma URL apareceu nos pixels da screenshot.
- Não usar título estruturado pelo OCR quando ele for uma URL ou trecho de barra de endereço; procurar uma linha de conteúdo útil como fallback.
- URLs de origem são associadas quando chegam explicitamente no compartilhamento (`EXTRA_TEXT` ou texto de `ClipData`). Se não vierem, manter a captura como imagem com OCR e sem link de origem presumido.
- Não solicitar overlay sobre outros aplicativos como solução padrão: além da permissão especial, o overlay não tem acesso confiável à URL/página em primeiro plano.

## Possibilidade de fluxo pós-screenshot
O fluxo mais confiável é usar a ação **Compartilhar** do próprio Android após a captura de tela e escolher CaptureFlow. O Android documenta que o payload pode conter a imagem em `EXTRA_STREAM` e a URL associada em `EXTRA_TEXT`; quando o app de origem fornece ambos, CaptureFlow consegue associar o link e usar o fluxo atual de metadados. Nem todos os apps/dispositivos fornecem a URL associada, então o fluxo deve continuar funcionando como captura de imagem + OCR quando ela estiver ausente.

Um botão flutuante desenhado pelo CaptureFlow sobre qualquer app é tecnicamente possível apenas com permissão especial de sobreposição (`SYSTEM_ALERT_WINDOW`) concedida pelo usuário, mas **não resolve por si só a obtenção do link**: Android isola os apps e não expõe genericamente a URL da página visível. Além disso, sobreposições podem ser bloqueadas por apps sensíveis e geram uma permissão intrusiva. Preferir uma ação clara na notificação de screenshot ou instruir o usuário a usar o compartilhamento nativo do screenshot. Não implementar scraping da tela de outro app nem depender de acessibilidade para inferir URL.


## Correção: título OCR e origem real da captura
O OCR continua ativo para ler a imagem e gerar um título útil, mas o texto reconhecido não deve ser tratado automaticamente como URL original. A barra de endereço do navegador, mensagens como “Not secure”, IPs locais, portas e caminhos podem aparecer no OCR sem representar o conteúdo/post capturado.
- Nunca preencher `sourceUrl` apenas porque uma URL apareceu nos pixels da screenshot.
- Não usar título estruturado pelo OCR quando ele for uma URL ou trecho de barra de endereço; procurar uma linha de conteúdo útil como fallback.
- URLs de origem são associadas quando chegam explicitamente no compartilhamento (`EXTRA_TEXT` ou texto de `ClipData`). Se não vierem, manter a captura como imagem com OCR e sem link de origem presumido.

## Possibilidade de fluxo pós-screenshot
O fluxo mais confiável é usar a ação **Compartilhar** do próprio Android após a captura e escolher CaptureFlow. O Android documenta que o payload pode conter a imagem em `EXTRA_STREAM` e a URL associada em `EXTRA_TEXT`; quando o app de origem fornece ambos, CaptureFlow consegue associar o link e usar o fluxo atual de metadados. Nem todos os apps/dispositivos fornecem a URL associada, então o fluxo deve continuar funcionando como captura de imagem + OCR quando ela estiver ausente.

Um botão flutuante sobre qualquer app exigiria permissão especial de sobreposição (`SYSTEM_ALERT_WINDOW`) concedida pelo usuário, mas não resolve por si só a obtenção do link: Android não expõe genericamente a URL da página visível a outro app. Preferir o compartilhamento nativo do screenshot ou uma ação na notificação. Não implementar scraping da tela de outro app nem depender de acessibilidade para inferir URL.

## Recuperação da origem de imagens e screenshots
A identificação da origem é feita em camadas, sem prometer que toda screenshot terá URL recuperável:

1. **Prioridade 1 — URL recebida junto com a imagem:** ao receber `ACTION_SEND` com MIME de imagem, o app procura uma URL HTTP(S) válida em `Intent.EXTRA_TEXT`. Quando presente, associa-a automaticamente a `sourceUrl`, marca `sourceVerified = true` e registra `sourceDetectionMethod = share_extra_text`. Não é necessário pedir ao usuário para copiar o endereço manualmente.
2. **Fallback — texto do ClipData:** se `EXTRA_TEXT` não trouxer URL, o app procura texto nos itens de `ClipData`. Uma URL encontrada é associada como origem recebida pelo compartilhamento (`share_clipdata_text`) e marcada como verificada pelo payload explícito.
3. **OCR da imagem:** continua sendo usado para ler o conteúdo e sugerir título. URLs encontradas nos pixels não são associadas como origem, pois podem pertencer à barra do navegador, a um IP local ou a conteúdo não relacionado.
4. **Indício do aplicativo de origem:** quando disponível, `Intent.EXTRA_REFERRER`/`EXTRA_REFERRER_NAME` pode fornecer um pacote `android-app://...`. Para imagens importadas de um URI MediaStore no Android 10+, `OWNER_PACKAGE_NAME` também pode ser registrado como indício. Isso preenche `sourceApp` e o método de detecção, mas **não prova qual página estava aberta e não substitui uma URL**. O valor pode identificar o componente que gravou o arquivo, não necessariamente o app de origem da captura.

Os campos `sourceApp`, `sourceDetectionMethod` e `sourceVerified` são metadados locais de proveniência. URLs recebidas explicitamente por compartilhamento são consideradas verificadas; OCR nunca confirma nem cria `sourceUrl`. A migração Room 5→6 adiciona esses campos sem descartar referências existentes. Referências antigas com `sourceUrl` são migradas como verificadas; registros antigos sem URL permanecem sem origem verificada.

**Limite conhecido:** screenshots detectadas automaticamente pelo MediaStore não contêm, por padrão, um campo padronizado com a URL da página. Se não houver URL compartilhada nem URL legível por OCR, o Capture Flow não inventará nem deduzirá uma URL a partir apenas do nome/pasta do arquivo ou do pacote do app.


**Limite:** o parser usa regex sobre o HTML recebido e não executa JavaScript. Páginas dinâmicas, autenticadas ou que bloqueiem requisições podem não fornecer H1/imagem. Não é um navegador/DOM completo.

## Imagens e OCR
- Photo Picker ou compartilhamento fornece URI; o app tenta copiar a imagem para armazenamento privado, mantendo a URI original como fallback em caso de falha.
- OCR usa ML Kit local. Heurísticas avaliam posição, tamanho, caixas delimitadoras, comprimento e sinais aproximados de interface de vídeo/social/notícias/compras/produtividade.
- Para referência de imagem, o título sugerido é persistido; texto OCR bruto não substitui o conteúdo principal. Erro de OCR não apaga a referência.
- Visualizador usa imagem ajustada, sem recorte. Não excluir imagens escolhidas manualmente da galeria.

## Monitoramento de screenshots
- Opcional e habilitado pelo usuário, com permissões de imagens e notificações quando aplicável.
- Foreground service observa MediaStore e procura nomes/pastas que parecem screenshots recentes; é best-effort e varia por fabricante/restrições do sistema.
- Não lê pixels de outros apps nem usa Accessibility Service.
- **Inconsistência existente:** o serviço de notificação ainda contém nomes e textos históricos de tarefa (`ACTION_CREATE_TASK`, “pronta para tarefa”), embora o app ativo seja a biblioteca de referências. Corrigir as strings sem reintroduzir uma tela independente de tarefas.

## Banco de dados
- `captures`: id, title, content, kind, category, `sourceUrl` opcional, `imageUri` opcional, createdAt, isCompleted, `sourceApp` opcional, `sourceDetectionMethod` opcional e `sourceVerified` booleano.
- `capture_subtasks`: id, captureId, title, isCompleted e createdAt; índice por captureId.
- `checklist_tasks`: entidade/DAO legado ainda presente, mas sem tela de tarefas independente na navegação atual.
- Versão Room no código: **6**. Migrações: 1→2 adiciona imageUri em tarefas; 2→3 description; 3→4 cria capture_subtasks/índice; 4→5 adiciona category em tarefas; 5→6 adiciona proveniência de origem em captures e marca URLs antigas existentes como verificadas. Atualizar contrato e testar upgrades ao mudar esquema.

## Privacidade e validação
- Dados e imagens permanecem no dispositivo; sem cloud sync.
- Links acionam requisição ao host fornecido pelo usuário; HTML limitado por timeout/tamanho. Download de capa acontece somente após confirmação de categoria.
- Validar em CI o commit exato e executar `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` em `android-native/`.
- Testar Sharesheet (URL, texto+URL, texto simples, imagem), imagem+URL em EXTRA_TEXT, fallback ClipData, referrer Android, OCR com URL válida/inválida e confirmação de candidato, migração Room 5→6, H1/metadata fallbacks, imagem relativa, redirects, timeout/offline, cancelamento, quatro categorias, duplicatas, permissões, screenshot notification, Back nativo e telas estreitas/fonte ampliada.
