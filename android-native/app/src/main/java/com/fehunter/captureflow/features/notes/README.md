# Notes

Feature responsável pela linha de pensamento associada a cada referência.

## Responsabilidades
- Criar, editar e excluir blocos ordenados com texto e imagem opcional.
- Exibir blocos no fullscreen em área de altura limitada, com rolagem vertical e até dois blocos visíveis simultaneamente.
- Abrir a edição de um bloco por toque prolongado; o botão de balão abre a gestão da coleção.
- Limitar o texto de cada bloco a 10.000 caracteres.

## Ownership e persistência
- Os dados continuam associados ao agregado Capture e persistidos pela feature captures no campo captures.notes já existente.
- NotesCodec serializa a lista em formato versionado e lê notas legadas em texto simples como um único bloco, sem migration destrutiva.
- A feature não acessa Room, DAOs ou repositórios de Captures.
- Imagens são copiadas para armazenamento privado pela fronteira Image Import usando sempre o perfil de compressão Alto.
- O coordinator conecta a UI de Notes aos callbacks de Captures e Image Import.

## Ponto de entrada
- presentation/components/NotesEditorDialog.kt: CRUD da linha de pensamento.
- domain/model/NoteBlock.kt e NotesCodec.kt: modelo e serialização retrocompatível.
