# Capture Flow — estado atual

**Auditoria documental:** 2026-11-04 | **Base:** main, 155a49b34eb3e536ade189ce7e84c1344bb44560. Este documento descreve o código auditado; não substitui CI nem testes em dispositivo.

## Produto e stack
Caderno pessoal Android local para guardar links, textos e imagens como referências organizadas, com categorias, subtarefas e anotações. App ativo em android-native/; legacy-expo/ é histórico. Kotlin, Jetpack Compose/Material 3, MVVM, Coroutines/Flow, Room/SQLite v11, ML Kit OCR e SharedPreferences. Sem conta/sincronização própria; links, feeds e páginas podem acessar serviços externos.

## Arquitetura e ownership
Feature-first + MVVM em um módulo Gradle. app/navigation compõe shell/Biblioteca; app/di faz DI manual; core contém contratos/utilitários compartilhados. Features ativas: captures (agregado, Room, subtarefas, mídias e notas), imageimport (Sharesheet, Photo Picker, câmera, metadados, OCR, screenshots), imagecompression, categories, search, dashboard, fullscreen, youtubeplayer, notes, newsletters, settings e backup. Domain não importa Android/Compose/Room/Data; Presentation não acessa DAO. Fullscreen usa callbacks de Captures; Notes e Backup consomem contratos públicos. checklist_tasks é legado; subtarefas ativas em capture_subtasks.

## Shell e tema
Biblioteca única, sem bottom navigation/tela independente de tarefas. Top bar oferece Newsletters, monitoramento screenshot, menu e tema. Menu: Privacidade, Tutoriais, Backup e restauração. Tema: Seguir dispositivo, Claro, Escuro. Newsletters/Backup substituem temporariamente a Biblioteca. A opção global Ocultar concluídos foi removida; concluídos continuam acessíveis. Direção visual Hybrid Personal Notebook conforme docs/design-system.md; viewfinder e fundo de mídia permanecem escuros.

## Biblioteca/Captures
Entradas: Fotografar (CameraX, foco por toque, recorte/revisão), Galeria (Photo Picker) e Nova tarefa (título obrigatório até 160 caracteres + categoria; cria texto sem imagem/URL/OCR). Sharesheet aceita links, texto e imagens. Categorias padrão: Ler depois, Verificar, Salvar, Importante; Todas é filtro. Categorias personalizadas podem ser criadas/renomeadas/excluídas; padrão protegidas. Filtro em duas linhas com rolagem horizontal compartilhada; Ver todas abre modal. Busca fica abaixo dos filtros. Cards de categoria exibem até três prévias pendentes, progresso e acesso à grade. Categoria expandida alterna Pendentes/Concluídas em grade de três colunas. Sem imagem usa TaskPaperCover; concluídas usam CompletedBadge. Conclusão isCompleted é independente da categoria. Dashboard conta tudo, sem depender da busca/filtro; cinco categorias inicialmente com expansão. Ordenação de categorias: manual, recentes, antigas, nome A–Z/Z–A. Ordenação de referências na grade: recentes, antigas, título A–Z/Z–A. Preferências distintas; preview mantém ordem original.

## Image Import/OCR/screenshots
Links HTTP(S) ficam pendentes até revisão e confirmação de categoria; cancelar não persiste. YouTube tenta oEmbed oficial para título/thumbnail; demais sites usam H1/metadados HTML e imagem article/main/body como fallback. Download de capa após confirmação. Parser não executa JS; páginas dinâmicas/autenticadas podem não fornecer metadados. OCR ML Kit é local e sugere título editável; OCR bruto não substitui conteúdo. Imagens são copiadas para storage privado quando possível; URI original é fallback. Nunca apagar imagem escolhida manualmente. Screenshot monitoring é opt-in, best-effort por metadados MediaStore; não lê pixels de outros apps, não usa Accessibility Service nem overlay. Notificação abre confirmação com prévia/categoria. Excluir screenshot original somente após confirmação e cópia privada bem-sucedida.

## Compressão
Médio JPEG quality 70; Alto quality 50; dimensão máxima 2560 px, correção EXIF, saída JPEG privada. 70/50 não garante redução percentual. Entradas URI file/content e bytes raster; file usa FileInputStream; content usa ContentResolver/fallback descritor. Notes usa Alto; recorte PNG lossless em memória enviado como ByteArray.

## Fullscreen, Web Reader e YouTube
HorizontalPager: swipe esquerda avança, direita retorna, sem loop; Back fecha camada superior. Imagem usa Fit em fundo escuro; título abaixo do header/sobre região superior da mídia com gradiente. Header contém Abrir link externo quando URL válida; dock contém Visualizar conteúdo (globo), sub-checklist, concluir, editar título/categoria e excluir conforme estado. Concluídas mostram só reabrir; Notes é independente no canto inferior esquerdo. Leitor web tem JS/DOM Storage off, acesso a arquivos/conteúdo e mixed content bloqueados, Safe Browsing e navegação HTTP/HTTPS com host válido; Back percorre histórico. YouTube usa player IFrame/WebView fora do pager, 16:9, autoplay off, controles on, Referer/origin explícitos e lifecycle; não contorna restrições. Image Import pode salvar thumbnail; player não baixa vídeo.

## Notes
Blocos ordenados por referência, texto até 10.000 caracteres e imagem opcional. Botão balão cria; toque longo edita/remove; toque simples na imagem abre preview proporcional. Recorte Original/1:1/4:5/16:9, PNG em memória e compressão Alto para JPEG privado. Dados versionados em captures.notes, com leitura de notas legadas; Notes não acessa Room.

## Newsletters
RSS 2.0/Atom, cadastro por URL de feed/site e descoberta. Room v11: newsletter_sources/newsletter_articles. Atualização manual de uma fonte ou todas; ETag/Last-Modified quando disponível. Fetch/parser limitados e validação de host. Leitor prioriza conteúdo do feed, pode abrir URL canônica quando há só resumo; Abrir original disponível. Salvar cria referência em Ler depois e vincula artigo. Retenção preserva artigos salvos. Sem scraping privado, bypass paywall, cache de imagens ou atualização periódica automática.

## Settings e Backup
Settings: tema system/light/dark, ordenação de categorias e referências, perfil de compressão. Privacidade e Tutoriais no menu. **Divergência de código:** InformationScreens.kt ainda chama backup de funcionalidade futura; corrigir UI em alteração separada. Backup acessível pelo menu; ZIP v1 com manifest.json e images/, exporta referências/categorias/conclusão/subtarefas/mídias/notas/imagens. SHA-256 deduplica imagens. Importação aditiva, sem apagar/substituir; categorias existentes preservam ordenação; duplicatas ignoradas por identidade implementada; colisões de IDs remapeadas; inserção Room transacional e limpeza de temporários em falha. Limites: manifesto 10 MB, imagem 25 MB, descompactado 250 MB, 50.000 registros/50.002 entradas. Não inclui Newsletters nem preferências.

## Persistência e validação
Room v11: captures, categories, capture_subtasks, capture_media, newsletter_sources, newsletter_articles; checklist_tasks legado. Auditoria documental não executou CI nem testes visuais. Validar no commit atual Sharesheet, permissões/foreground screenshot, Photo Picker/URI grants, compressão, player, WebViews e backup com arquivos grandes/conflitos.
