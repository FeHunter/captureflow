# Capture Flow — estado atual do aplicativo

**Atualizado:** 2026-10-30. Este documento registra o comportamento observado no código; não é uma promessa de release.

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
- Categorias atuais: `Ler depois`, `Verificar`, `Salvar`, `Importante` e a categoria fixa `Concluído`; `Todas` é somente filtro.
- `Concluído` é criada por padrão e não pode ser renomeada, excluída ou atribuída manualmente a uma nova referência.
- Ao concluir uma referência, ela é movida para `Concluído`; sua categoria anterior fica registrada para ser restaurada quando a conclusão for desfeita.
- O filtro `Concluído` exibe os itens concluídos mesmo quando a opção global de ocultar concluídos está ativa. Em `Todas`, a opção global continua controlando a visibilidade.
- Busca combina título/conteúdo com categoria selecionada.

## Meu progresso
- Dashboard no topo da biblioteca com o título **Meu progresso**.
- A categoria **Concluído** recebe destaque visual próprio: cartão com cor semântica de destaque do tema, ícone de conclusão e contador em tamanho ampliado.
- O destaque permanece visível mesmo sem itens concluídos; nesse estado, apresenta uma mensagem de incentivo.
- Abaixo do destaque, as demais categorias com referências são listadas com suas quantidades atuais. Categorias vazias são omitidas.
- Os números consideram todas as referências persistidas, sem depender da busca, do filtro atual ou da opção de ocultar concluídos.
- O contador de conquistas considera as referências cuja categoria é `Concluído`; a mudança de layout não altera as regras de persistência ou conclusão.

## Grupos e thumbnails de referência
- A tela principal agrupa referências por categoria em cartões de largura total; cada grupo mostra até três thumbnails e a ação “Ver todas”.
- Ao expandir uma categoria, as referências aparecem em grade de três colunas. Cada thumbnail usa `ContentScale.Crop`, título sobreposto em degradê escuro e placeholder quando não há imagem acessível.
- Estado de OCR pode exibir indicador de processamento/erro.
- O botão/gesto Back fecha a categoria expandida quando nenhum visualizador está por cima.

## Anotações da referência
- O visualizador fullscreen ativo mantém uma coluna vertical de ações sobre a imagem, na ordem: concluir, editar título, anotações e excluir.
- **Anotações** abre um diálogo dentro da experiência fullscreen, com campo multilinha e limite de 10.000 caracteres.
- Quando há anotação não vazia, o visualizador aberto pelo cartão também a apresenta sobre a imagem no canto inferior esquerdo, em um painel preto translúcido com texto branco (até cinco linhas visíveis e reticências para conteúdo maior). A anotação continua editável/removível pelo botão **Anotações**.
- Salvar persiste o texto em `captures.notes` por meio do ViewModel/DAO. Texto vazio limpa as anotações; Cancelar não persiste alterações.
- No Details Fullscreen ativo (`selectedCapture`), anotações não vazias também aparecem sobre a imagem no canto inferior esquerdo, em painel preto translúcido com texto branco (até cinco linhas visíveis); o painel acompanha a captura atual durante o swipe. A coluna de ações permanece no canto inferior direito.
- As anotações pertencem à referência e continuam disponíveis após fechar/reabrir o visualizador.

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
- A confirmação apresenta prévia da imagem e exige que o usuário escolha uma categoria cadastrada, exceto **Concluído**, antes de habilitar **Salvar referência**. Essa escolha explícita é requisito funcional e não deve ser substituída por uma categoria fixa.
- Ao confirmar, a imagem é copiada para o armazenamento privado e criada na categoria escolhida; o arquivo original só é removido após cópia privada bem-sucedida.
- Requer monitoramento ativado e permissões de imagens/notificações aplicáveis à versão Android.

## Monitoramento de screenshots
- Opcional e habilitado pelo usuário, com permissões de imagens e notificações quando aplicável.
- Foreground service observa MediaStore e procura nomes/pastas que parecem screenshots recentes; é best-effort e varia por fabricante/restrições do sistema.
- Não lê pixels de outros apps nem usa Accessibility Service.
- **Inconsistência existente:** o serviço de notificação ainda contém nomes e textos históricos de tarefa (`ACTION_CREATE_TASK`, “pronta para tarefa”), embora o app ativo seja a biblioteca de referências. Corrigir as strings sem reintroduzir uma tela independente de tarefas.

## Banco de dados
- `captures`: id, title, content, kind, category, `sourceUrl` opcional (URL original usada pelo botão de abrir link no fullscreen), `imageUri` opcional, `notes` opcional (anotações de texto do usuário), createdAt, isCompleted e `previousCategory` opcional (categoria restaurada ao desfazer conclusão).
- `capture_subtasks`: id, captureId, title, isCompleted e createdAt; índice por captureId.
- `checklist_tasks`: entidade/DAO legado ainda presente, mas sem tela de tarefas independente na navegação atual.
- Versão Room no código: **8**. Migrações recentes: 5→6 adiciona persistência de categorias; 6→7 adiciona `previousCategory`, cria a categoria fixa `Concluído` e move referências já concluídas para ela preservando a categoria anterior; 7→8 adiciona a coluna opcional `notes` para anotações de texto por referência. A conclusão/desconclusão é persistida no DAO e a categoria anterior é restaurada ao reabrir uma referência.

## Privacidade e validação
- Dados e imagens permanecem no dispositivo; sem cloud sync.
- Links acionam requisição ao host fornecido pelo usuário; HTML limitado por timeout/tamanho. Download de capa acontece somente após confirmação de categoria.
- Validar em CI o commit exato e executar `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` em `android-native/`.
- Testar Sharesheet (URL, texto+URL, texto simples, imagem), H1/metadata fallbacks, imagem relativa, redirects, timeout/offline, cancelamento, quatro categorias, duplicatas, OCR, permissões, screenshot notification, Back nativo e telas estreitas/fonte ampliada.


## Estado da migração Feature-first + MVVM

A primeira etapa de organização de pacotes moveu a persistência e apresentação principal de Captures para `features/captures`, as integrações de OCR/metadados/screenshots para `features/imageimport` e o armazenamento privado compartilhado para `core/storage`. O banco Room foi dividido em entidades, DAOs, database/migrations e classificador. `AppContainer` e `CaptureViewModelFactory` fazem a composição manual das dependências; `MainActivity` continua na raiz do pacote como entrada Android.

Esta é uma etapa estrutural, não a conclusão da arquitetura. O ViewModel ainda agrega operações de Captures, categorias, subtarefas e importação; a tela Compose ainda reúne múltiplos fluxos; Room entities, DAOs, database/migrations e classificador já estão separados em arquivos, mas continuam sob a camada Data. A próxima etapa deve extrair Domain, contratos, repositórios e UseCases, preservando o schema Room v8 e os fluxos funcionais atuais.
