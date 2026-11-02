# Notes

Feature responsável pela linha de pensamento associada a cada referência.

## Responsabilidades
- Criar, editar e excluir blocos ordenados com texto e imagem opcional.
- Exibir blocos no fullscreen em painel que acompanha a altura real do conteúdo; a rolagem vertical é aplicada somente quando o conjunto ultrapassa o limite de 248 dp.
- O botão de balão abre diretamente o formulário de nova anotação. Toque prolongado em qualquer área do bloco existente, inclusive sobre a imagem, abre seu formulário de edição/remoção, sem uma tela intermediária de gerenciamento. Toque simples na imagem abre uma visualização fullscreen proporcional; um ícone de expansão indica essa ação.
- Limitar o texto de cada bloco a 10.000 caracteres.
- Ao selecionar ou trocar uma imagem, abrir uma etapa de recorte (Original, 1:1, 4:5 e 16:9). Após o recorte, a UI codifica o resultado como PNG lossless em memória (`ByteArray`) e entrega os bytes ao `ImageCompressionService` no perfil Alto. Não é criado PNG temporário no disco nem URI intermediária; a anotação recebe somente a URI JPEG privada após compressão bem-sucedida.
- Ao retornar do Photo Picker, a imagem é copiada imediatamente para um arquivo temporário no cache privado enquanto a permissão temporária da URI ainda está disponível; a prévia/recorte decodifica esse snapshot local, sem depender da validade posterior da URI do provedor. A leitura de `content://` tenta `ContentResolver` e `ParcelFileDescriptor` como fallback e, se ambos falharem, preserva a causa técnica no erro. URIs `file://` são abertas diretamente por `FileInputStream`. O arquivo de staging é removido após a decodificação e a prévia anterior é liberada antes de carregar uma nova seleção.

## Ownership e persistência
- Os dados continuam associados ao agregado Capture e persistidos pela feature captures no campo captures.notes já existente.
- CaptureNotesCodec serializa a lista em formato versionado e lê notas legadas em texto simples como um único bloco, sem migration destrutiva.
- A feature não acessa Room, DAOs ou repositórios de Captures.
- O compressor é responsável por criar o JPEG final em `files/captures`; a UI não grava arquivos de imagem finais. Se a compressão falhar, a seleção e o recorte permanecem disponíveis para nova tentativa.
- O coordinator conecta a UI de Notes aos callbacks de Captures e Image Import.

## Ponto de entrada
- presentation/components/NotesEditorDialog.kt: formulário direto de criação/edição, seleção, staging, prévia, recorte e envio dos bytes PNG em memória ao compressor compartilhado.
- O modelo `CaptureNote` e o `CaptureNotesCodec` pertencem ao domínio de Captures, pois as anotações integram o agregado da referência.
