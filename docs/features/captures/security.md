# Segurança e privacidade — Captures

- Referências, sub-checklists e imagens persistidas ficam no dispositivo; não há sincronização cloud implementada.
- OCR é executado localmente via ML Kit; não enviar imagem/texto capturado a serviço de IA externo.
- Metadados de links requerem requisição de rede à URL fornecida pelo usuário. Manter validação de esquema/host, limites de tempo e volume e revisar riscos de SSRF/redirecionamento se o serviço mudar.
- Copiar imagens para diretório privado reduz dependência de permissões temporárias do provedor. Se a cópia falhar, URI original é fallback e pode expirar/deixar de ser acessível.
- Ao excluir, remover apenas arquivos privados que o app reconhece como seus; nunca apagar conteúdo externo do MediaStore/provedor.
- Photo Picker é o caminho manual preferido para evitar permissão ampla de imagens.
- O monitor de screenshots é opt-in e baseado em metadados MediaStore (nome/pasta/data); não lê pixels de outros apps e não usa Accessibility Service.
- Monitoramento em segundo plano usa foreground service tipo `specialUse`; antes de publicar, validar política vigente do Google Play, justificativa e declaração/revisão exigidas.
- Permissões negadas, force-stop, economia de bateria e particularidades OEM podem interromper a detecção.
- Não documentar monitoramento como garantido ou permanente em todos os dispositivos.
