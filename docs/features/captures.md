# Captures

The feature receives, classifies, persists, searches and manages link, text and image references.

## Current native baseline
- Manual text/link entry.
- In-app image selection through Android's system Photo Picker; selected images are copied to app-private storage when possible and added as local captures.
- Android Sharesheet `ACTION_SEND` for `text/plain` and `image/*`, including image URIs supplied through `EXTRA_STREAM` or `ClipData`.
- Shared images are copied to app-private storage before persistence when the provider allows reading them.
- Basic title/category suggestions and exact-content duplicate avoidance for text; each image is retained as its own capture.
- Room persistence, search, category filters, completion and deletion.
- On-demand Google ML Kit OCR runs on device; extracted text replaces the generic image placeholder and is saved locally.
- OCR displays processing, success, empty-result and error feedback.

## Image analysis flow
1. The user taps **Selecionar imagem** in the References screen.
2. Android's system Photo Picker lets the user choose a photo or screenshot without requesting broad photo-library permission.
3. CaptureFlow copies the selected image into app-private storage when possible and saves a local capture.
4. The user taps **Analisar imagem / extrair texto** on that capture to run on-device OCR.

## Rules and gaps
- Keep source app and source URL optional unless verified.
- OCR output is extracted content, not verified metadata; users should review it before relying on it.
- No captured content or image is uploaded.
- Automatic screenshot discovery is not implemented; users select an image in CaptureFlow or share an image/screenshot to the app explicitly.
- If copying an image fails, the original URI is retained as a fallback; OCR may fail if the provider no longer grants access.


## Screenshots como tarefas

- A aba **Checklist** permite selecionar uma imagem usando o seletor de fotos do Android e salvá-la como uma tarefa chamada "Revisar screenshot".
- Imagens compartilhadas de outros aplicativos para o CaptureFlow também são registradas como tarefas com screenshot anexado.
- O arquivo é copiado para o armazenamento privado do aplicativo quando possível; não há envio para a nuvem.
- A exclusão da tarefa também tenta excluir a cópia local da imagem.
- O banco Room migra da versão 1 para a 2 adicionando o campo opcional `imageUri` às tarefas existentes, preservando os dados já salvos.
- A captura automática de screenshots em segundo plano não faz parte desta implementação. O usuário escolhe a imagem ou usa o menu Compartilhar do aplicativo de origem.
