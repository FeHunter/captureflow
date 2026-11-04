# Captures

Feature responsável pelo ciclo de vida das referências salvas.

## Estrutura
- `domain/model`: `Capture`, `CaptureCategory`, `CaptureSubtask`, `CaptureNote` e `CaptureMedia`.
- `domain/repository`: contrato `CaptureRepository`.
- `domain/usecase`: operações de aplicação e regras de criação.
- `data/local`: entidades, DAOs e Room, incluindo `CaptureMediaEntity`/`CaptureMediaDao`.
- `data/mapper`: conversão Room↔Domain.
- `data/repository`: implementação Room.
- `presentation/viewmodel`: estado e ações de Captures.
- `presentation/components`: `CategoryCard`, `CategoryThumbnail` e o selo reutilizável `CompletedBadge` para referências concluídas.
- A sub-checklist de cada referência é apresentada no fullscreen e permanece vinculada ao `captureId`.

O ViewModel consome `CaptureUseCases`; não deve importar entidades, DAOs ou implementações Data. A conclusão é independente da categoria: concluir/reabrir altera somente `isCompleted`. Os cartões exibem somente referências pendentes na prévia, mantendo a contagem de concluídas/total e a ação para abrir todos os itens. Em `Todas`, cartões sem pendentes são omitidos; no filtro de categoria específica, o cartão continua visível com estado vazio. `CompletedBadge` é aplicado nos thumbnails de referências concluídas. A categoria expandida permite alternar entre Pendentes e Concluídas. A coluna Room `previousCategory` permanece somente como legado de compatibilidade, sem uso na regra de negócio. A biblioteca é composta pelo `CaptureLibraryCoordinator` em `app/navigation`, que integra as features sem criar dependências entre suas camadas de Presentation. Categories, Search, Dashboard, Image Import e Fullscreen mantêm seus componentes e estados próprios.


## Múltiplas mídias

- `CaptureMedia` representa uma imagem local vinculada a uma referência, com posição estável e indicação de capa.
- A migration Room 9→10 cria `capture_media` e importa `captures.imageUri` existente como a primeira capa, sem remover a coluna legada.
- O limite é de 10 mídias por referência. O import de metadados de links coleta até 10 imagens, limita cada download a 10 MB e valida MIME/decodificação antes de gravar.
- O fullscreen oferece navegação por controles explícitos e permite definir a capa; a capa também atualiza `captures.imageUri` para compatibilidade.


## Tarefas manuais e ações de concluídas

- A criação manual é apresentada na Biblioteca junto de Fotografar e Galeria. Exige título (máximo 160 caracteres) e categoria existente.
- `CaptureFactory.createTask` cria uma referência `kind=text`, com `imageUri=null` e `sourceUrl=null`; não dispara OCR, compressão ou acesso à mídia.
- A exclusão de referência concluída na grade usa confirmação e o mesmo callback de exclusão do Fullscreen.
- Referências sem imagem exibem a capa de papel pautado de `core/ui/components/TaskPaperCover.kt`; é um desenho Compose, sem arquivo de imagem persistido.
- O ViewModel valida a categoria com a fonte de dados atual antes de persistir a tarefa.


## Leitura interna de páginas web

- O Fullscreen mantém a ação **Abrir link** para o navegador externo e oferece **Visualizar conteúdo** para páginas HTTP/HTTPS com host válido.
- A leitura interna usa uma WebView dedicada à feature Fullscreen, sem compartilhar estado de navegação com Newsletters.
- Configuração alinhada ao leitor de Newsletters: JavaScript e DOM Storage desativados; acesso a arquivos/conteúdo e conteúdo misto bloqueados; somente navegação HTTP/HTTPS com host válido é permitida dentro da WebView.
- O Back do Android navega pelo histórico da WebView antes de retornar ao Fullscreen; o botão de voltar fecha o leitor interno.
- URLs de vídeo do YouTube continuam usando o player dedicado e não recebem a ação genérica de WebView.
