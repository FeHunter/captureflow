# Integrações — Captures

## Android Sharesheet / Intents
`MainActivity` recebe payloads suportados por Android Share Intent, incluindo texto/URLs e URI de imagem (`EXTRA_STREAM`/ClipData conforme suporte do código). Conteúdo compartilhado é encaminhado ao ViewModel. O formulário manual de texto/link não faz parte da tela atual.

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


## OCR e screenshots

`OcrService` agora prioriza candidatos a título entre 30% e 72% da altura da imagem, com pontuação adicional pela proximidade do centro. A heurística volta aos demais candidatos quando não encontra título plausível na região central.

No fluxo de screenshot aberto pela notificação, a imagem é copiada para armazenamento privado e a referência é persistida antes de solicitar a exclusão do original via MediaStore. A exclusão pode exigir confirmação do sistema e não é garantida se o usuário cancelar ou o Android negar acesso. Importações manuais pelo Photo Picker não removem a imagem original.
