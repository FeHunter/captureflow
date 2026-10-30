# Feature: Image Import

**Responsabilidade:** receber e preparar imagens/links compartilhados, OCR e integração de screenshots.

## Organização
- `data/ocr`: reconhecimento local e heurísticas de OCR.
- `data/metadata`: extração HTTP(S) de metadados de links.
- `data/android`: monitoramento MediaStore e notificação de screenshots.
- `domain` e `presentation`: serão extraídos ao separar o fluxo de importação do ViewModel legado.

## Regras
- OCR permanece on-device.
- Falhas de OCR/metadados não descartam a captura.
- Importação manual não exclui a imagem original.
- A limpeza da screenshot original só pode ocorrer após confirmação explícita da notificação e cópia privada bem-sucedida.

## Estado da migração
Serviços foram realocados para esta feature; a coordenação de importação ainda está no ViewModel transitório de Captures. O próximo passo é criar um contrato público de resultado de importação e um ViewModel próprio.
