# Testes e critérios de aceite — Capture Flow

**Atualizado:** 2026-11-04. Plano de regressão; confirmar CI e dispositivo no commit em teste.

## Biblioteca e Captures
- [ ] Nova tarefa exige título/categoria e cria item textual sem imagem/URL/OCR.
- [ ] Fotografar abre CameraX, foco por toque, captura, recorte e revisão.
- [ ] Galeria/Photo Picker e Sharesheet mantêm fluxos de revisão.
- [ ] Categorias padrão protegidas; CRUD de categoria personalizada e reatribuição ao excluir.
- [ ] Filtros em duas linhas, modal Ver todas e busca abaixo dos filtros.
- [ ] Cards mostram previews/progresso; grade expandida alterna Pendentes/Concluídas.
- [ ] Concluir/reabrir não muda categoria; Dashboard ignora busca/filtros.
- [ ] Ordenação de categoria e de referência são preferências independentes.
- [ ] Sem imagem usa TaskPaperCover; concluída usa CompletedBadge.
- [ ] Subtarefas persistem por captureId e são removidas ao excluir referência.

## Image Import e Compression
- [ ] OCR sugere título editável; OCR vazio/falho não descarta imagem.
- [ ] Link exige confirmação de categoria; cancelar não persiste.
- [ ] Testar oEmbed YouTube, thumbnail, H1/metadados e fallback.
- [ ] Testar redirects, offline, timeout, página dinâmica e URL inválida.
- [ ] Perfis Médio/Alto, EXIF, transparência, imagens grandes e URI file/content.
- [ ] Falha de compressão mantém seleção/recorte; nunca apagar origem manual.
- [ ] Screenshot opt-in, permissões, notificação, confirmação e cópia privada.

## Fullscreen, Notes e YouTube
- [ ] Pager swipe/Back; imagem Fit; título visível abaixo do header.
- [ ] Header Abrir link e dock Visualizar conteúdo; fallback externo para sites incompatíveis.
- [ ] YouTube incorporável, indisponível e não incorporável; lifecycle WebView/retry.
- [ ] Notes texto/imagem, edição por toque longo, recorte, compressão Alto e retry.
- [ ] Subtarefas e conclusão sincronizam com Biblioteca.

## Newsletters, Backup e Settings
- [ ] RSS 2.0/Atom direto e descoberta; feed inválido, host privado e redirect.
- [ ] Atualização manual, ETag/Last-Modified, retenção e preservação de artigos salvos.
- [ ] Leitor de feed e fallback URL canônica; salvar artigo em Ler depois.
- [ ] Backup export/import, ZIP inválido, path traversal, tamanho máximo e imagem ausente.
- [ ] Reimportação, duplicatas, colisões de IDs, remapeamento e limpeza em falha.
- [ ] Confirmar que backup v1 não inclui Newsletters/preferências.
- [ ] Tema system/light/dark e preferências persistem após reiniciar.

## Validação técnica
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew assembleDebugAndroidTest`
- [ ] Testar Android 13+ e fabricante com gestão agressiva de bateria.
- [ ] Validar acessibilidade, fontes ampliadas, contraste e telas estreitas.
