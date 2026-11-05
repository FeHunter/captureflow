# Integrações — Capture Flow

**Atualizado:** 2026-11-04.

- **Sharesheet/Intents:** MainActivity recebe texto, URLs e URI(s) de imagem; SharedContentIntentParser adapta payloads na fronteira de Image Import. Links exigem revisão/categoria/confirmação; imagens seguem OCR/revisão. Texto manual é suportado por Nova tarefa.
- **Photo Picker e câmera:** Photo Picker evita permissão ampla da galeria. CameraX tem foco por toque e etapa de recorte/revisão. Imagens são copiadas para storage privado quando possível; URI original é fallback.
- **ML Kit OCR:** executado localmente. OcrService gera linhas/caixas; TitleDetectionService sugere título editável. OCR bruto não substitui conteúdo principal.
- **Metadados HTTP(S):** Image Import tenta oEmbed oficial para YouTube; outros sites usam H1, metadados sociais e imagens de conteúdo. Não executa JavaScript; páginas dinâmicas/autenticadas/bloqueadas podem não fornecer metadados. Falha não descarta URL; download da capa ocorre após confirmação.
- **Screenshots:** monitoramento opt-in via MediaStore/notificação, best-effort por metadados/nome/pasta. Não usa Accessibility Service, não lê pixels de outros apps e não cria overlay. Permissões/OEM/bateria podem interromper. Origem só é removida após confirmação e cópia privada bem-sucedida.
- **WebViews:** Fullscreen e Newsletters têm leitores separados com JS/DOM Storage/file access desativados e mixed content bloqueado. YouTube Player usa IFrame dedicado com Referer/origin, controles e autoplay desativado.
