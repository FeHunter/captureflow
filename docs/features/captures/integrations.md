# Integrações — Captures

## Android Sharesheet / Intents
`MainActivity` recebe payloads suportados por Android Share Intent, incluindo texto/URLs e URI de imagem (`EXTRA_STREAM`/ClipData conforme suporte do código). Conteúdo compartilhado é encaminhado ao ViewModel. O formulário manual de texto/link não faz parte da tela atual.



## Recuperação da origem de imagens e screenshots
A identificação da origem é feita em camadas, sem prometer que toda screenshot terá URL recuperável:

1. **Prioridade 1 — URL recebida junto com a imagem:** ao receber `ACTION_SEND` com MIME de imagem, o app procura uma URL HTTP(S) válida em `Intent.EXTRA_TEXT`. Quando presente, associa-a automaticamente a `sourceUrl`, marca `sourceVerified = true` e registra `sourceDetectionMethod = share_extra_text`. Não é necessário pedir ao usuário para copiar o endereço manualmente.
2. **Fallback — texto do ClipData:** se `EXTRA_TEXT` não trouxer URL, o app procura texto nos itens de `ClipData`. Uma URL encontrada é associada como origem recebida pelo compartilhamento (`share_clipdata_text`) e marcada como verificada pelo payload explícito.
3. **OCR da imagem:** se não houver URL compartilhada, o OCR local procura uma URL HTTP(S) no texto reconhecido. A URL é armazenada como candidata (`sourceDetectionMethod = ocr_candidate`, `sourceVerified = false`), nunca tratada silenciosamente como confirmada. Ao tocar no ícone de link do fullscreen, a UI pede confirmação antes de marcar a origem como verificada e abrir o endereço.
4. **Indício do aplicativo de origem:** quando disponível, `Intent.EXTRA_REFERRER`/`EXTRA_REFERRER_NAME` pode fornecer um pacote `android-app://...`. Para imagens importadas de um URI MediaStore no Android 10+, `OWNER_PACKAGE_NAME` também pode ser registrado como indício. Isso preenche `sourceApp` e o método de detecção, mas **não prova qual página estava aberta e não substitui uma URL**. O valor pode identificar o componente que gravou o arquivo, não necessariamente o app de origem da captura.

Os campos `sourceApp`, `sourceDetectionMethod` e `sourceVerified` são metadados locais de proveniência. URLs recebidas explicitamente por compartilhamento são consideradas verificadas; URLs de OCR exigem confirmação. A migração Room 5→6 adiciona esses campos sem descartar referências existentes. Referências antigas com `sourceUrl` são migradas como verificadas; registros antigos sem URL permanecem sem origem verificada.

**Limite conhecido:** screenshots detectadas automaticamente pelo MediaStore não contêm, por padrão, um campo padronizado com a URL da página. Se não houver URL compartilhada nem URL legível por OCR, o Capture Flow não inventará nem deduzirá uma URL a partir apenas do nome/pasta do arquivo ou do pacote do app.


## Photo Picker
O seletor de imagem do sistema é usado para importar uma imagem sem solicitar permissão ampla de leitura de toda a galeria no fluxo manual. A URI é copiada para armazenamento privado quando possível.

## Google ML Kit Text Recognition
OCR roda localmente no aparelho. `OcrService` processa blocos/linhas, caixas delimitadoras e perfil heurístico de UI para sugerir título e descrição. A referência de imagem atual persiste o título sugerido, não o OCR bruto no campo `content`. Resultado pode variar por layout/idioma/qualidade da imagem.

## Metadados HTTP(S)
`LinkMetadataService` busca título e descrição em HTML público. Não executa JavaScript nem scraping visual; redirecionamentos automáticos estão desativados. Sites autenticados, bloqueados ou dependentes de JS podem não fornecer metadados.

## Screenshot monitor
`ScreenshotMonitor` observa MediaStore enquanto a UI está em primeiro plano. `ScreenshotNotificationService` implementa monitoramento opcional em foreground service e notificação persistente. Ambos reconhecem screenshots por metadados de nome/pasta e têm limitações por fabricante, permissões e política de bateria. Não usam Accessibility Service.

## Persistência
Room/SQLite é local. Não há sincronização cloud ou upload de imagem para OCR. O serviço de metadados de link faz uma requisição de rede à URL compartilhada.


## Extração de metadados de link (estado atual)
- `HttpURLConnection`, redirects habilitados, timeout de 4 s para conexão e leitura, leitura máxima de 512 KB.
- Título: H1 → og:title → twitter:title → title do documento → host.
- Imagem: img de article → img de main → og:image/og:image:url/twitter:image.
- Resolve URL relativa contra a URL final da página.
- O parser não executa JavaScript; páginas dinâmicas/autenticadas podem não expor conteúdo. O link continua salvável com fallback.
- Download de imagem ocorre somente após o usuário confirmar a categoria.
