# Capture Flow — estado atual do aplicativo

**Atualizado:** 2026-11-01. Este documento registra o comportamento observado no código; não é uma promessa de release.

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
- Ordem: cartão para selecionar imagem/gerenciar monitoramento, busca com botão limpar, filtro de categorias em até duas linhas e biblioteca de cartões por categoria com rolagem vertical. O filtro oferece modal com a lista completa.
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

## Anotações da referência
- O visualizador fullscreen ativo mantém uma coluna vertical de ações sobre a imagem: concluir, sub-checklist, abrir link (quando disponível), editar, excluir; anotações permanecem no canto inferior esquerdo.
- **Anotações** abre um diálogo dentro da experiência fullscreen, com campo multilinha e limite de 10.000 caracteres.
- Quando há anotação não vazia, o visualizador aberto pelo cartão também a apresenta sobre a imagem no canto inferior esquerdo, em um painel preto translúcido com texto branco (até cinco linhas visíveis e reticências para conteúdo maior). A anotação continua editável/removível pelo botão **Anotações**.
- Salvar persiste o texto em `captures.notes` por meio do ViewModel/DAO. Texto vazio limpa as anotações; Cancelar não persiste alterações.
- No Details Fullscreen ativo (`selectedCapture`), anotações não vazias também aparecem sobre a imagem no canto inferior esquerdo, em painel preto translúcido com texto branco (até cinco linhas visíveis); o painel acompanha a captura atual durante o swipe. A coluna de ações permanece no canto inferior direito.
- As anotações pertencem à referência e continuam disponíveis após fechar/reabrir o visualizador.

## Visualizador fullscreen ativo
- É aberto ao tocar em um thumbnail dentro de uma categoria expandida.
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
- OCR usa ML Kit local. Heurísticas avaliam posição, tamanho, caixas delimitadoras, comprimento e sinais aproximados de interface de vídeo/social/notícias/compras/produtividade.
- Para referência de imagem, o título sugerido é persistido; texto OCR bruto não substitui o conteúdo principal. Erro de OCR não apaga a referência.
- Visualizador usa imagem ajustada, sem recorte. Não excluir imagens escolhidas manualmente da galeria.

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
- `captures`: id, title, content, kind, category, `sourceUrl` opcional, `imageUri` opcional, `notes` opcional, createdAt e `isCompleted`. A coluna legada `previousCategory` continua no schema para compatibilidade, mas é limpa e não participa mais das regras de negócio.
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

Room permanece v8. Não há sincronização cloud e as regras local-first continuam preservadas. A validação final depende de CI verde no HEAD mais recente e revisão manual dos fluxos existentes.


## Atualização — câmera e metadados do YouTube
- A câmera CameraX aceita toque no preview para iniciar foco automático na região selecionada.
- Após fotografar, a pessoa pode manter a imagem original ou aplicar recorte central nos formatos 1:1, 4:5 ou 16:9 antes de iniciar OCR e revisão.
- Para links do YouTube, a extração tenta primeiro o endpoint oficial oEmbed para obter título e thumbnail; em falha, mantém o fallback HTML já existente. O fluxo de confirmação e a URL original permanecem inalterados.
