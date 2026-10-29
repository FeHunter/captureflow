# UI/UX atual — Capture Flow

**Fonte de verdade:** `android-native/app/src/main/java/com/fehunter/captureflow/ui/CaptureFlowApp.kt`. Ver também [estado atual completo](../../current-state.md).

## Identidade e navegação
- Nome do produto: **Capture Flow** (barra superior e launcher).
- Tela única de referências; sem bottom navigation e sem tela independente de tarefas.
- Menu superior: mostrar/ocultar concluídas e alterar tema.
- Tema: seguir sistema, claro ou escuro; paleta azul-marinho/oceano/ciano definida em `MainActivity.kt`.

## Ordem e comportamento da tela principal
1. Cartão para analisar imagem do dispositivo, selecionar imagem e configurar/ativar/desativar monitoramento de screenshots.
2. Busca por título e conteúdo, com X para limpar.
3. Chips horizontais: Todas, Ler depois, Verificar, Salvar, Importante.
4. Grupos de categoria em cartões de largura total, rolagem vertical. Cada grupo mostra até três thumbnails e “Ver todas”.
- O filtro e a busca atuam juntos. “Todas” é filtro, não categoria persistida.
- Categorias usadas em filtros/seletores: Ler depois, Verificar, Salvar, Importante.

## Thumbnails e categoria expandida
- Thumbnail quadrado com `ContentScale.Crop`, título sobreposto em degradê escuro e placeholder “Sem imagem” quando não há capa.
- Estado de OCR pode exibir ícone de processamento/erro.
- “Ver todas” abre modal fullscreen da categoria, com botão de voltar/fechar, nome da categoria e contagem.
- Grade expandida usa três colunas, espaçamento uniforme e rolagem vertical.
- Tocar em thumbnail abre o visualizador fullscreen.

## Visualizador fullscreen ativo
- Barra superior com data/hora, chip de categoria, botão de abrir link original quando `sourceUrl` for uma URL HTTP(S) válida, checkbox de conclusão, excluir e fechar.
- A imagem usa `ContentScale.Fit` em fundo preto, sem recorte/estiramento.
- Título fica sobre a região superior da imagem com gradiente preto forte, esmaecendo até transparente.
- Dica “‹ Deslize para navegar ›” é uma sobreposição na parte inferior central; não reserva altura.
- Swipe-left avança; swipe-right retorna dentro da mesma categoria filtrada. Não há loop nas extremidades.
- A data/categoria/estado de conclusão e a imagem acompanham a página estabilizada.
- Back nativo fecha primeiro o visualizador e mantém a categoria expandida; Back novamente retorna à biblioteca. Botões explícitos continuam disponíveis.

## Captura de link compartilhado
- Receber URL via Sharesheet inicia busca de metadados no HTML.
- Diálogo “Salvar link como referência” exibe título, URL, descrição quando disponível e carregamento.
- Categoria é obrigatória, sem seleção padrão: Ler depois, Verificar, Salvar ou Importante.
- Salvar fica desabilitado durante o fetch. Cancelar descarta a captura sem persistir.
- Se o fetch falhar, o link pode ser salvo com título fallback. A imagem é baixada após confirmar.
- Texto compartilhado sem URL continua sendo salvo diretamente; imagem compartilhada usa importação/OCR.

## Origem da imagem e links detectados
- Ao compartilhar uma imagem acompanhada de URL em `EXTRA_TEXT`, a origem é associada automaticamente. Se o texto do payload não trouxer URL, o app tenta usar texto em `ClipData`.
- Se a URL for encontrada somente por OCR, o ícone de link no fullscreen abre um diálogo de confirmação com o endereço identificado. “Confirmar e abrir” marca a URL como verificada e abre o app/navegador padrão; “Cancelar” não a confirma nem abre.
- O ícone de link só aparece quando há URL HTTP(S) válida. A proveniência por pacote de aplicativo é informativa e não garante a página de origem.

## Seleção de imagem e screenshots
- Photo Picker retorna uma imagem; a UI pede categoria antes de importar.
- Monitor de screenshots é opcional e pede permissões apropriadas. Detecção é best-effort via metadados do MediaStore.
- O modal de screenshot confirma antes de salvar; não excluir imagens selecionadas manualmente.
- Inconsistência conhecida: algumas strings da notificação ainda dizem “tarefa”, embora a ação atual salve referência.

## Cartão de referência e sub-checklist
- O código contém um composable `CaptureCard` não referenciado pelo fluxo ativo; o layout ativo é a biblioteca por grupos e thumbnails.
- A sub-checklist ativa abre em `ModalBottomSheet`, mostra progresso, itens com checkbox, exclusão e campo para adicionar.
- O banco persiste itens por `captureId`. Não há tela independente de tarefas.

## Acessibilidade e validação
- Alvos de toque de ícone devem permanecer em torno de 48 dp.
- Manter descrições de ícones, contraste do título e suporte a tema claro/escuro.
- Validar largura estreita, fonte ampliada, gestos, Back nativo, URLs com falha e permissões em dispositivo real.
