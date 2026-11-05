# Desempenho e carregamento de mídia

**Atualizado:** 2026-11-05

## Objetivo

Manter a Biblioteca fluida com coleções maiores e reduzir picos de memória ao exibir capas e imagens de anotações, sem alterar persistência ou adicionar dependências.

## Implementações

- Biblioteca: LazyColumn permanece como container principal; categorias usam chaves estáveis e contentType.
- Grade expandida: LazyVerticalGrid mantém composição sob demanda, com chave estável por Capture e contentType para thumbnails.
- Pager Fullscreen: HorizontalPager mantém páginas compostas sob demanda; a lista de referências do pager é memoizada pelos dados que realmente a alteram.
- Consultas visuais: o conjunto filtrado é agrupado por categoria uma única vez por atualização, evitando filtrar toda a coleção para cada card de categoria.
- Estado de scroll: a visibilidade das floating actions usa derivedStateOf, reduzindo recomposições em cada pixel de rolagem.
- Lifecycle: coletores da Biblioteca usam collectAsStateWithLifecycle.
- Imagens: ImageBitmapLoader decodifica em dispatcher de background, faz downsampling de acordo com o destino e mantém cache LRU limitado a 1/8 do heap (entre 4 e 24 MiB). Cards usam até 360 px, prévias de Notes até 480 px e Fullscreen (incluindo a ampliação de imagens de Notes) até 1600 px por dimensão.

## Limites e decisões

- Room e arquivos privados continuam sendo a fonte da verdade; cache de bitmaps é descartável e não persiste dados.
- O cache considera URI, dimensão solicitada e tamanho/data de modificação para arquivos locais. Novas capas recebem URI de arquivo novo e não reutilizam a entrada anterior.
- Nenhum arquivo original é reprocessado e nenhum schema/migration foi alterado.
- Não foi adicionada biblioteca externa de imagens: o pipeline atual já cobre file:// e content://, então a primeira etapa foca em cache e downsampling.
- O cache LRU limita o conjunto retido, mas o consumo real também depende de bitmaps atualmente referenciados pela UI. Validar em dispositivos com memória restrita e durante navegação rápida no pager.

## Validação

- testDebugUnitTest
- assembleDebug
- assembleDebugAndroidTest
- Teste manual: biblioteca com muitas referências, troca rápida de categorias, grade expandida, pager Fullscreen, substituição de capa e Notes com várias imagens.
