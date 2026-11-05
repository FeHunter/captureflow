# Acesso a dados e ações — Captures

**Atualizado:** 2026-11-04.

## Estado e persistência
CaptureViewModel expõe referências, subtarefas, mídias e operações por Flow/StateFlow. Consome CaptureUseCases e não acessa DAO/Room diretamente. RoomCaptureRepository implementa CaptureRepository.

## Operações
- Criar referência manual via CaptureFactory: título obrigatório, categoria válida, kind=text, sem imagem/URL/OCR.
- Criar/importar imagem: Image Import coordena OCR/storage e solicita persistência pelo contrato público de Captures.
- Editar título/categoria, concluir/reabrir e excluir via UseCases.
- Adicionar/editar/excluir mídias e definir capa; limite de 10 mídias por referência.
- Adicionar/concluir/excluir subtarefas por captureId.
- Importar lote via CaptureImportBatch para Backup; inserção Room transacional.

## Ownership cross-feature
- Categories aplica regras CRUD/seleção pelo contrato CaptureRepository.
- Search filtra dados fornecidos por Captures.
- Dashboard agrega fluxos, sem persistência própria.
- Fullscreen usa callbacks de Captures.
- Notes altera blocos do agregado por callbacks; não acessa Room.
- Image Import possui OCR, metadados, cópia de imagem e screenshot.
- Backup possui ZIP/manifesto e chama operações públicas de Captures.

## Arquivos e privacidade
Image Import/Image Compression são responsáveis pela cópia/compressão de arquivos. Captures mantém URIs de mídia e metadados. Ao excluir referência, subtarefas/mídias relacionadas são removidas; limpeza física só pode apagar arquivos privados pertencentes ao app. Nunca apagar URIs externas do provedor/MediaStore.
