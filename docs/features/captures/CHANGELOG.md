# Changelog — Captures

## 2026-10-29 — Recuperação em camadas da URL de origem
- Prioridade 1: associa automaticamente URL HTTP(S) recebida em `EXTRA_TEXT` junto com imagem compartilhada.
- Fallback de texto: procura URL em itens textuais de `ClipData`.
- OCR local identifica URL visível como candidata não verificada; o fullscreen pede confirmação explícita antes de marcar como verificada e abrir.
- Registra `sourceApp`, `sourceDetectionMethod` e `sourceVerified`; referrer Android e `MediaStore.OWNER_PACKAGE_NAME` são apenas indícios de pacote, não URLs nem prova da página.
- Migração Room 5→6 preserva registros e marca URLs preexistentes como verificadas.
- Documentados limites, privacidade e casos de teste.

## Atualização — botão de link no fullscreen
- Reaproveita `CaptureEntity.sourceUrl`, já presente no modelo/payload, para manter a URL original sem duplicar campos.
- Adiciona no header do visualizador fullscreen um botão para abrir o link no app/navegador padrão; aparece apenas quando a URL é HTTP(S) válida.
- Atualiza contrato funcional, UI/UX e wireframe.


Este changelog resume mudanças relevantes da feature. Para o estado atual e pendências, consulte [a visão geral do projeto](../../project-overview.md).

## v0.3.0 — Versão, OCR visual e capas
- Cabeçalho usa BuildConfig para exibir a versão.
- OCR retorna texto reconhecido e sugestão heurística de título/descrição.
- Cartões de referência exibem imagem como capa.
- Documentação da feature organizada em arquivos dedicados.

## Correções OCR e cartões
- Heurística de título usa caixas delimitadoras, posição e altura de linhas.
- Linhas vizinhas de manchetes em múltiplas linhas podem ser agrupadas.
- Filtros para ruído de interface de vídeo/social/notícias/compras/produtividade.
- OCR automático para imagens adicionadas como referências; a imagem original permanece associada.
- Estado de processamento/erro sem apagar a referência.
- Cartão nominal de 320 dp com capa de 104 dp.

## Fullscreen e sub-checklist
- Visualização fullscreen usa imagem ajustada com `ContentScale.Fit`.
- Swipe horizontal entre referências: esquerda próxima, direita anterior; sem loop.
- Sub-checklist persistente em tabela Room `capture_subtasks`, com adicionar/concluir/remover.
- Acesso à sub-checklist a partir do cartão e do detalhe fullscreen.

## Tema e biblioteca
- Tema sistema/claro/escuro.
- Busca com ação para limpar consulta.
- Filtros por categoria e grade de duas colunas.
- Formulário manual de texto/link e tela independente de tarefas não fazem parte da tela principal atual.

## v0.3.2 — Detecção assistida de screenshots
- Monitor de MediaStore em primeiro plano com detecção best-effort por nome/pasta.
- Pedido explícito de permissão e confirmação para importar screenshot.

## v0.3.3 — Notificações de screenshots
- Serviço foreground opt-in com notificação persistente e alertas para screenshots detectadas.
- A implementação histórica usou nomes de ação/linguagem de tarefa.

## 2026-10-29 — Tela única de Referências e swipe hint
- PR #24 removeu a navegação inferior/tela independente de Tarefas.
- Screenshot aberta pela notificação é salva como `CaptureEntity` na biblioteca Referências via `addImageCapture(uri, category = "Referência")`.
- Dica `‹ Deslize para navegar ›` sobreposta na área de imagem fullscreen, sem reservar espaço nem alterar `ContentScale.Fit`.
- Documentação atualizada para declarar que a sub-checklist pertence a cada referência.
- PR #25 adiciona imports Compose ausentes `KeyboardOptions` e `ImeAction`; aguardando CI/build do commit correspondente.
- Pendências conhecidas: strings da notificação/modal ainda usam linguagem de tarefa e categoria `Referência` não aparece na lista padrão de categorias.


## 2026-10-29 — UX fullscreen

- Substituída a detecção manual de arraste horizontal por `HorizontalPager` para transição de página mais fluida.
- Sincronizados cabeçalho e página selecionada; navegação permanece limitada à lista atual, sem wrap-around.
- Substituído o atalho de sub-checklist no cabeçalho fullscreen por **Editar título**.
- Adicionado salvamento do título manual via ViewModel e atualização específica no DAO, preservando conteúdo e imagem.
- Sub-checklist segue disponível pelo controle do cartão.


## 2026-10-29 — Fullscreen UX

- Replaced manual horizontal drag detection with Compose `HorizontalPager`.
- Kept filtered-list boundaries and synchronized the fullscreen header with the settled page.
- Replaced fullscreen sub-checklist shortcut with an **Editar título** action.
- Added title-only persistence through ViewModel and DAO; the sub-checklist remains available from the card.


## 2026-10-29 — OCR centralizado e fluxo de screenshots
- Priorizada a região central da imagem na pontuação de candidatos a título OCR.
- Fixada a divisão do fullscreen em 30% de cabeçalho e 70% de imagem, com fade escuro sobre a imagem para títulos longos.
- Adicionada remoção best-effort do screenshot original após importação confirmada pelo fluxo de notificação; imagens escolhidas manualmente permanecem na galeria. A exclusão pode ser impedida pelo Android sem consentimento do usuário.


## 2026-10-29 — Ajuste de proporção do fullscreen
- Alterada a proporção do detalhe fullscreen de 30/70 para 20% de cabeçalho e 80% de imagem, ampliando a área visual da referência.


## 2026-10-29 — Tags, prévia de links e layout
- Padronizadas as tags e opções de filtro para Ler depois, Verificar, Salvar e Importante.
- Links compartilhados agora tentam extrair título, descrição e imagem Open Graph/Twitter; a imagem é salva localmente quando possível.
- Ajustada a altura dos cartões compactos para 272 dp e a proporção do fullscreen para 20% de cabeçalho e 80% de imagem.


## 2026-10-29 — Contraste do título e back nativo
- Reforçado o degradê escuro atrás do título sobre a imagem, preservando dimensões, enquadramento e estrutura do fullscreen.
- Adicionado tratamento explícito de Back do Android no visualizador de referência e na categoria expandida. A navegação fecha apenas a camada superior, mantendo a categoria aberta quando o usuário volta do visualizador.
- Mantidos os botões visíveis de fechar/voltar como alternativa acessível.


## 2026-10-29 — Swipe no visualizador de categoria e nome do app
- Restaurado `HorizontalPager` no visualizador aberto a partir dos thumbnails de categoria, com swipe-left para avançar e swipe-right para voltar dentro da categoria filtrada, sem loop.
- Sincronizados os metadados e ações da captura com a página estabilizada; mantidos gradiente, enquadramento e Back nativo.
- Restaurado o nome do produto para **Capture Flow** na barra superior e no rótulo do aplicativo Android.


## 2026-10-29 — Estado atual documentado e confirmação de categoria de links
- Criado `docs/current-state.md` como inventário funcional do aplicativo, incluindo arquitetura, UI/UX, wireframes, persistência, OCR, imagens, compartilhamento e limitações.
- O compartilhamento de URL agora abre um diálogo de prévia e exige escolha explícita entre Ler depois, Verificar, Salvar e Importante antes de persistir.
- `LinkMetadataService` faz requisição HTTP(S) do body HTML com timeout e limite de 512 KB; prioriza H1 para título e imagem de article/main para capa, com metadados Open Graph/Twitter como fallback.
- Após confirmação, tenta baixar a imagem para armazenamento privado. Se metadados falharem, o link pode ser salvo com título de fallback.
- Atualizados contratos de arquitetura, UI/UX, wireframes, segurança e teste.
