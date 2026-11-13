# Capture Flow — estado atual

**Atualizado:** 2026-11-13 | **Base de implementação:** `main` + branch `feat/notes-camera-fullscreen-crop-newsletter-home`.

## Produto e stack
Caderno pessoal Android local para guardar links, textos e imagens como referências organizadas, com categorias, subtarefas, anotações, newsletters e compartilhamento. Kotlin, Jetpack Compose/Material 3, MVVM, Coroutines/Flow, Room v11, ML Kit OCR e SharedPreferences. Sem conta/sincronização própria.

## Arquitetura e ownership
Feature-first + MVVM em um módulo Gradle, DI manual. Features ativas: captures, imageimport, imagecompression, categories, search, dashboard, fullscreen, youtubeplayer, notes, newsletters, settings, backup e share. Captures é proprietária do agregado e persistência; Notes e Backup consomem seus contratos. Share recebe um payload snapshot e exporta sem alterar a origem.

## Biblioteca
Biblioteca única, sem bottom navigation. Top bar: Newsletters, monitoramento de screenshot, menu e tema. Entradas Fotografar/Galeria/Nova tarefa ficam no header; após rolar para baixo, aparecem como floating actions bottom-center. A Home apresenta Dashboard, prévia com até três artigos recentes da fonte Newsletter selecionada e Categorias, nessa ordem. A prévia lê os artigos já persistidos e abre o leitor existente; Ver todas acessa o gerenciamento completo de feeds. Quatro categorias padrão, filtros, busca, cards e grade expandida; conclusão é independente da categoria.

## Image Import e Compressão
Câmera CameraX, Photo Picker, Sharesheet, links com metadados, OCR local e screenshot monitoring opt-in. Link exige revisão e categoria. Thumbnail pode ser obtida e salva após confirmação. Downloads de preview agora passam pelo pipeline de compressão. Notes reutiliza o CameraX e faz o recorte no editor de anotações, evitando uma segunda tela de crop.

ImageCompressionService decodifica URI file/content ou bytes, aplica EXIF e limite de 2560 px. Imagens opacas geram candidatos WebP lossy e JPEG e o menor arquivo é persistido. PNG e imagens com alpha usam WebP lossless, com PNG lossless como fallback de encoder; fotografias opacas comparam WebP/JPEG. Perfis: Médio (JPEG 70/WebP 78) e Alto (JPEG 52/WebP 66); números são qualidade de encoder, não percentuais de redução. Arquivos existentes não são migrados automaticamente; novas importações e novas capas usam o pipeline atualizado.

## Fullscreen e Notes
Fullscreen mantém pager, header, dock, leitor web e YouTube. Header inclui Abrir link e compartilhar. O dock vertical abre o modal de edição de título/categoria, que também oferece a ação Alterar imagem da capa. A troca abre o Photo Picker, exige recorte antes da compressão e só substitui a capa anterior após sucesso; o recorte aceita content:// e file:// e respeita EXIF. Notes exibe blocos; o cabeçalho do painel permite expandir/recolher a área de leitura sem alterar dados. O editor permite anexar imagem pela Galeria ou câmera CameraX, com recorte e compressão Alto antes de persistir.

## Newsletters
RSS 2.0/Atom, fontes/artigos locais em Room v11, atualização manual, leitor e salvamento na Biblioteca. A Home apresenta três artigos recentes da fonte selecionada, sem fetch adicional, e abre o mesmo leitor. Backup v2 inclui metadados de fontes, artigos, estado de leitura e vínculo savedCaptureId. Importação reconhece fontes pelo feedUrl e artigos pelos índices únicos, sem substituir dados locais existentes.

## Backup
ZIP v2 com manifest.json e images/. Exporta Captures, categorias, subtarefas, mídias, anotações e Newsletters. SHA-256 deduplica arquivos de imagem. Importa versões 1 e 2: v1 segue compatível sem arrays Newsletter; v2 mescla fontes/artigos de forma aditiva. Preferências Settings continuam fora do backup. Limites de tamanho/entradas permanecem conforme contrato Backup.

## Share
Feature nova com payload de título, categoria, data, link, conteúdo, capa, mídias e blocos Notes/imagens. Ações: copiar texto, compartilhar texto e imagens privadas pelo Android Sharesheet, exportar PDF e RTF via Storage Access Framework. PDF/RTF incluem capa, imagens relacionadas e imagens das anotações. FileProvider expõe somente files/captures com permissão temporária de leitura.

## Desempenho
Biblioteca e grade expandida usam containers lazy com chaves estáveis; itens da grade declaram contentType. O agrupamento por categoria é calculado uma vez por atualização do conjunto filtrado, evitando varreduras repetidas da lista para cada seção. O estado da barra flutuante deriva do scroll via derivedStateOf, e os coletores da Biblioteca usam collectAsStateWithLifecycle. core/ui/ImageBitmapLoader aplica downsampling por dimensão de destino e cache LRU limitado a uma fração do heap; previews usam bitmaps menores que o Fullscreen. O cache é transitório e não substitui arquivos privados/Room.

## Persistência e validação
Room v11: captures, categories, capture_subtasks, capture_media, newsletter_sources e newsletter_articles; checklist_tasks é legado. Esta alteração não exige migration. CI deve validar unit tests, assembleDebug, instrumentation compile e bundleRelease; teste real de câmera, Photo Picker, crop de capa, cancelamento, leitor de Newsletter e temas claro/escuro permanece necessário em dispositivo/emulador.

## Compartilhamento de referências — atualização 2026-11-05

O Android Sharesheet compartilha uma referência como um único PDF (application/pdf) gerado a partir do snapshot de Share. O documento inclui informações textuais, capa, galeria na ordem do payload e Notes com imagens. Exportação PDF/RTF via SAF e cópia de texto continuam disponíveis. O padrão de layout do PDF está documentado em docs/features/share/README.md.
