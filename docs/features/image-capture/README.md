# Image Capture — plano e implementação

**Status:** primeira implementação em revisão (PR).  
**Feature proprietária:** Image Import.  
**Data:** 31/10/2026.

## Objetivo e decisões

Permitir transformar fotografias do cotidiano em referências no Capture Flow, sem criar uma tela independente de tarefas. A câmera é própria do app e usa CameraX. Após fotografar, o usuário revisa a imagem, o título sugerido pelo OCR e a categoria antes de salvar. Photo Picker e imagens recebidas pelo Android Sharesheet usam o mesmo fluxo de revisão.

- Categorias vêm do domínio; `Concluído` não pode ser atribuída a uma nova referência.
- A imagem é copiada para armazenamento privado antes da análise, quando possível.
- OCR é local via ML Kit. Falha/ausência de texto não bloqueia título manual.
- Nenhuma referência é persistida antes da confirmação.
- Cancelamento remove somente arquivos temporários do app.
- Sem upload, cloud sync ou migração Room.

## Plano

1. Entrada: câmera CameraX integrada à biblioteca; manter Photo Picker e Sharesheet.
2. Preparação: copiar imagem para armazenamento privado e gerar rascunho pendente.
3. OCR: executar fora da main thread e sugerir título com fallback editável.
4. Revisão: prévia, título editável, categoria e estados de carregamento/erro.
5. Persistência: criar `Capture` por `CaptureFactory` e salvar via `CaptureUseCases`.
6. Limpeza: descartar somente arquivo temporário próprio ao cancelar.
7. Validação: testes unitários, assembleDebug e testes em dispositivo.

## Arquitetura

```text
CaptureLibrary (App composition)
  ├── ImageCaptureScreen (Presentation / CameraX)
  └── ImageImportViewModel
        ├── PendingImageReview (UI state)
        ├── ImageImportGateway (Domain contract)
        ├── ImageImportStorageService (Data)
        ├── OcrService (Data / ML Kit)
        └── CaptureUseCases (public Captures domain boundary)
              └── CaptureRepository -> Room
```

A câmera é componente de Presentation da Image Import. O App coordena sua abertura; o ViewModel mantém o rascunho e delega OCR/cópia ao gateway. A persistência usa o contrato público de Captures, sem importar Data/Room de outra feature.

## Critérios de aceite

- [ ] A câmera solicita permissão somente quando o usuário inicia a captura.
- [ ] A prévia aparece e permite tirar uma foto.
- [ ] A foto abre revisão e não é salva automaticamente.
- [ ] O título sugerido pode ser editado.
- [ ] O usuário escolhe uma categoria válida antes de salvar.
- [ ] OCR vazio/falho mantém a imagem e permite título manual.
- [ ] Cancelar não cria referência e remove somente arquivo temporário próprio.
- [ ] Foto, galeria e imagem compartilhada convergem para a revisão.
- [ ] A referência salva aparece na biblioteca com a imagem como capa.
- [ ] Nenhuma migration ou upload é introduzido.

## Fora do escopo desta entrega

- Widget Android e atalho de captura na tela inicial do dispositivo.
- Floating overlay, por exigir permissão especial e serviço persistente.
- Seleção de lente, flash, zoom e captura múltipla.
- A importação de screenshots detectadas por notificação mantém o fluxo existente para preservar a regra opt-in de exclusão da imagem original.

A validação final exige CI no commit do PR e teste em dispositivo físico; a API do repositório não substitui testes de hardware/permissões.
