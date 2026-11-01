# Notes

Feature responsável pela linha de pensamento associada a cada referência.

## Responsabilidades
- Criar, editar e excluir blocos ordenados com texto e imagem opcional.
- Exibir blocos no fullscreen em painel que acompanha a altura real do conteúdo; a rolagem vertical é aplicada somente quando o conjunto ultrapassa o limite de 248 dp.
- O botão de balão abre diretamente o formulário de nova anotação. Toque prolongado em um bloco existente abre seu formulário de edição/remoção, sem uma tela intermediária de gerenciamento.
- Limitar o texto de cada bloco a 10.000 caracteres.
- Ao selecionar ou trocar uma imagem, abrir uma etapa de recorte (Original, 1:1, 4:5 e 16:9). **Modo de diagnóstico temporário:** após o recorte, salvar o bitmap diretamente como PNG no diretório privado `files/captures`, sem chamar `ImageCompressionService`. Isso permite testar seleção, recorte, persistência e leitura da anotação isoladamente. A compressão será reativada após a validação.
- Ao retornar do Photo Picker, a imagem é copiada imediatamente para um arquivo temporário no cache privado enquanto a permissão temporária da URI ainda está disponível; a prévia/recorte decodifica esse snapshot local, sem depender da validade posterior da URI do provedor. A leitura de `content://` tenta `ContentResolver` e `ParcelFileDescriptor` como fallback e, se ambos falharem, preserva a causa técnica no erro. URIs `file://` são abertas diretamente por `FileInputStream`. O arquivo de staging é removido após a decodificação e a prévia anterior é liberada antes de carregar uma nova seleção.

## Ownership e persistência
- Os dados continuam associados ao agregado Capture e persistidos pela feature captures no campo captures.notes já existente.
- CaptureNotesCodec serializa a lista em formato versionado e lê notas legadas em texto simples como um único bloco, sem migration destrutiva.
- A feature não acessa Room, DAOs ou repositórios de Captures.
- No modo de diagnóstico atual, a própria UI grava o PNG recortado em `files/captures`; o arquivo continua sob a regra de armazenamento privado e é removido quando descartado/removido da anotação.
- O coordinator conecta a UI de Notes aos callbacks de Captures e Image Import.

## Ponto de entrada
- presentation/components/NotesEditorDialog.kt: formulário direto de criação/edição, seleção de imagem, recorte e salvamento PNG direto (compressão temporariamente desativada).
- O modelo `CaptureNote` e o `CaptureNotesCodec` pertencem ao domínio de Captures, pois as anotações integram o agregado da referência.
