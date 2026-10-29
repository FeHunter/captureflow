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


## Proveniência da origem em imagens
- URL recebida em `EXTRA_TEXT` ou `ClipData` é validada como HTTP(S) e associada à imagem compartilhada.
- URLs reconhecidas por OCR não são salvas como origem. O OCR pode ler a barra do navegador, endereços locais ou texto sem relação com o conteúdo; apenas URL explícita no payload de compartilhamento é associada automaticamente.
- Pacote vindo de referrer ou `MediaStore.OWNER_PACKAGE_NAME` é apenas metadado de proveniência; não deve ser tratado como prova da página nem convertido em URL.
- A URL de origem pode apontar para conteúdo externo; abrir o link sempre é uma ação explícita do usuário.

## Requisição de metadados de link
- O app requisita URLs HTTP(S) recebidas via Sharesheet, com timeout de conexão/leitura e limite de 512 KB de HTML.
- O parser lê HTML como texto; não executa scripts nem JavaScript.
- URLs de imagem são validadas como HTTP(S), resolvidas contra a URL final e baixadas após confirmação da categoria.
- Falha de rede não descarta a URL original; cancelar a confirmação não persiste a referência.
