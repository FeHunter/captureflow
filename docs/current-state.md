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
- Ao expandir uma categoria, as referências aparecem em grade de três colunas. Cada thumbnail usa `ContentScale.Crop` e placeholder quando não há imagem acessível.
- Título truncado em até duas linhas. O código atual mostra `capture.title` também no texto secundário; não tratar esse segundo campo como descrição independente.
- Menu de categoria, data/hora local `dd/MM/yyyy · HH:mm`, checkbox de conclusão, botão de sub-checklist com contagem e excluir.
- Concluídas podem ser ocultadas. Excluir uma referência também remove suas subtarefas e somente arquivos de imagem privados dentro do diretório próprio do app.

## Visualizadores fullscreen
Existem dois caminhos de visualização:
1. **Aberto pelo cartão:** cabeçalho ~20%, imagem ~80%; título/data/categoria, ação **Editar título** e fechar. A imagem usa `ContentScale.Fit`. O `HorizontalPager` permite swipe-left para próxima e swipe-right para anterior, respeitando a lista filtrada recebida pelo cartão, sem loop.
2. **Aberto pelo thumbnail da categoria:** tela de imagem fullscreen, data/categoria e ações concluir/excluir/fechar; swipe horizontal navega apenas pelas capturas da mesma categoria filtrada, sem loop. Título sobre imagem com gradiente preto forte até transparente e dica de swipe sobreposta sem reservar espaço.
- Back nativo fecha a camada superior. Fechar o viewer não deve fechar a categoria subjacente; outro Back retorna à biblioteca.
- Editar título persiste somente `captures.title`. Manter proporções, gradiente, `ContentScale.Fit` e controles explícitos.

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

## Monitoramento de screenshots
- Opcional e habilitado pelo usuário, com permissões de imagens e notificações quando aplicável.
- Foreground service observa MediaStore e procura nomes/pastas que parecem screenshots recentes; é best-effort e varia por fabricante/restrições do sistema.
- Não lê pixels de outros apps nem usa Accessibility Service.
- **Inconsistência existente:** o serviço de notificação ainda contém nomes e textos históricos de tarefa (`ACTION_CREATE_TASK`, “pronta para tarefa”), embora o app ativo seja a biblioteca de referências. Corrigir as strings sem reintroduzir uma tela independente de tarefas.

## Banco de dados
- `captures`: id, title, content, kind, category, sourceUrl opcional, imageUri opcional, createdAt e isCompleted.
- `capture_subtasks`: id, captureId, title, isCompleted e createdAt; índice por captureId.
- `checklist_tasks`: entidade/DAO legado ainda presente, mas sem tela de tarefas independente na navegação atual.
- Versão Room no código: **5**. Migrações: 1→2 adiciona imageUri em tarefas; 2→3 description; 3→4 cria capture_subtasks/índice; 4→5 adiciona category em tarefas. Atualizar contrato e testar upgrades ao mudar esquema.

## Privacidade e validação
- Dados e imagens permanecem no dispositivo; sem cloud sync.
- Links acionam requisição ao host fornecido pelo usuário; HTML limitado por timeout/tamanho. Download de capa acontece somente após confirmação de categoria.
- Validar em CI o commit exato e executar `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` em `android-native/`.
- Testar Sharesheet (URL, texto+URL, texto simples, imagem), H1/metadata fallbacks, imagem relativa, redirects, timeout/offline, cancelamento, quatro categorias, duplicatas, OCR, permissões, screenshot notification, Back nativo e telas estreitas/fonte ampliada.
