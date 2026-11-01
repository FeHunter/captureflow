# Notes

Feature responsável pela linha de pensamento associada a cada referência.

## Responsabilidades
- Criar, editar e excluir blocos ordenados com texto e imagem opcional.
- Exibir blocos no fullscreen em painel que acompanha a altura real do conteúdo; a rolagem vertical é aplicada somente quando o conjunto ultrapassa o limite de 248 dp.
- O botão de balão abre diretamente o formulário de nova anotação. Toque prolongado em um bloco existente abre seu formulário de edição/remoção, sem uma tela intermediária de gerenciamento.
- Limitar o texto de cada bloco a 10.000 caracteres.
- Ao selecionar ou trocar uma imagem, abrir uma etapa de recorte (Original, 1:1, 4:5 e 16:9); depois do recorte, comprimir no perfil Alto antes de salvar. Em caso de falha, manter a imagem e o recorte selecionados para permitir nova tentativa, exibindo o motivo retornado pelo serviço.

## Ownership e persistência
- Os dados continuam associados ao agregado Capture e persistidos pela feature captures no campo captures.notes já existente.
- CaptureNotesCodec serializa a lista em formato versionado e lê notas legadas em texto simples como um único bloco, sem migration destrutiva.
- A feature não acessa Room, DAOs ou repositórios de Captures.
- Imagens são copiadas para armazenamento privado pela fronteira Image Import usando sempre o perfil de compressão Alto.
- O coordinator conecta a UI de Notes aos callbacks de Captures e Image Import.

## Ponto de entrada
- presentation/components/NotesEditorDialog.kt: formulário direto de criação/edição, seleção de imagem, recorte e compressão da linha de pensamento.
- O modelo `CaptureNote` e o `CaptureNotesCodec` pertencem ao domínio de Captures, pois as anotações integram o agregado da referência.
