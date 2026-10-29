# Arquitetura do Capture Flow

## Stack canônica
Kotlin, Android SDK, Jetpack Compose/Material 3, ViewModel, Coroutines/Flow, Room/SQLite e Google ML Kit Text Recognition. O app ativo está em `android-native/`; `legacy-expo/` é histórico.

## Camadas e responsabilidades
- **Entrada Android — `MainActivity.kt`:** recebe launcher, `ACTION_SEND` do Sharesheet e intents de notificação; encaminha compartilhamentos ao ViewModel.
- **Apresentação — `ui/CaptureFlowApp.kt`:** tela única de referências, busca, chips, cartões, diálogo de categoria para links, seleção de imagem, fullscreen e sub-checklist.
- **Orquestração — `ui/CaptureViewModel.kt`:** estado observável, deduplicação, captura pendente de link, persistência, OCR, enriquecimento de link e subtarefas.
- **Dados — `data/CaptureDatabase.kt`:** entidades, DAOs, Room, classificação e normalização.
- **Serviços — `services/LinkMetadataService.kt`:** requisição HTTP(S), leitura limitada de HTML e extração H1/imagem/metadados.
- **Serviços — `services/OcrService.kt`:** OCR local e heurísticas de título.
- **Serviços — `services/SharedImageStorageService.kt`:** cópia/download para armazenamento privado e exclusão segura de arquivos privados.
- **Screenshots — `ScreenshotMonitor.kt` e `ScreenshotNotificationService.kt`:** detecção heurística pelo MediaStore e notificação opt-in.

```text
Sharesheet / Photo Picker / notification
                 |
                 v
            MainActivity
                 |
                 v
        CaptureFlowApp (Compose)
                 |
                 v
          CaptureViewModel
          /      |       \
         v       v        v
       Room     OCR    LinkMetadataService
      SQLite   ML Kit  HTTP(S) HTML
                         |
                         v
                SharedImageStorageService
```

## Fluxo de link compartilhado
1. `MainActivity` recebe `ACTION_SEND` com `text/plain` e chama `receiveSharedIntent`.
2. O ViewModel extrai a URL HTTP(S), define `PendingSharedLink` e inicia a requisição em coroutine; o banco ainda não recebe uma referência de link.
3. `LinkMetadataService` baixa o body HTML com timeout e limite de 512 KB. Prioridade de título: primeiro `H1`, `og:title`, `twitter:title`, `title`, host como fallback.
4. Prioridade de imagem: primeira imagem dentro de `article`, depois `main`, depois metadados Open Graph/Twitter. URLs relativas são resolvidas pela URL final após redirects.
5. A UI exibe título, URL, descrição quando disponível, estado de carregamento e seleção obrigatória entre quatro categorias.
6. Cancelar descarta a captura pendente. Confirmar valida categoria, evita duplicata por `sourceUrl`, grava referência e tenta baixar a imagem para armazenamento privado.
7. Se a requisição falhar, o usuário ainda pode salvar o link com o título de fallback.

A extração é baseada em regex e HTML recebido; não executa JavaScript nem equivale a um browser/DOM completo. Requisição bloqueada, site autenticado ou conteúdo dinâmico podem limitar o resultado.

## Texto, imagem compartilhada e recuperação de origem
- Texto sem URL é normalizado, deduplicado por conteúdo e persistido sem diálogo de categoria de link.
- Para imagem compartilhada, a prioridade é URL HTTP(S) em `EXTRA_TEXT`; o fallback é texto dos itens de `ClipData`. URLs recebidas explicitamente são associadas automaticamente e marcadas como verificadas.
- Se o payload não trouxer URL, o OCR local tenta encontrar um endereço na imagem. Esse endereço é candidato não verificado; o ícone de link no fullscreen solicita confirmação antes de marcá-lo como origem confirmada e abrir o navegador.
- `EXTRA_REFERRER`/`EXTRA_REFERRER_NAME` e, no Android 10+, `MediaStore.MediaColumns.OWNER_PACKAGE_NAME` podem fornecer apenas um indício de pacote de aplicativo. Não inferir uma URL a partir desse pacote nem afirmar que ele é necessariamente o app visível na screenshot.
- A seleção de imagem do Photo Picker segue fluxo semelhante. Não apagar a imagem original da galeria em importação manual.

### Metadados de proveniência
`CaptureEntity` persiste `sourceUrl`, `sourceApp`, `sourceDetectionMethod` e `sourceVerified`. A migração Room 5→6 mantém os registros existentes; URLs antigas são marcadas como verificadas, registros sem URL permanecem não verificados. A identificação é best-effort: uma screenshot descoberta apenas por nome/pasta no MediaStore não fornece, por si só, a URL da página.

## OCR
ML Kit reconhece texto localmente. Heurísticas analisam caixas delimitadoras, posição, tamanho e sinais aproximados de tipo de UI para sugerir título. Falhas não devem remover a referência nem a imagem.

## Dados e privacidade
- Banco local `captureflow.db`; versão Room 6.
- `captures` contém referências; `capture_subtasks` guarda subtarefas por `captureId`; `checklist_tasks` permanece como modelo legado/compatibilidade sem tela independente atual.
- Sem sync cloud.
- Metadados exigem requisição de rede ao URL compartilhado; imagens extraídas são baixadas localmente quando possível.
- O monitor de screenshots é opt-in e usa metadados MediaStore; não usa Accessibility Service nem lê pixels de outros apps.

## Regras de manutenção
- Não acessar DAOs diretamente de Composables.
- Não persistir link compartilhado antes da seleção de categoria.
- Manter a extração de H1 e imagem best-effort, com fallback e timeouts.
- Atualizar visão geral, estado atual, feature contract, UI/UX, wireframes, segurança, testes e changelog quando mudar fluxo ou layout.
- Executar `testDebugUnitTest` e `assembleDebug` quando o ambiente permitir; não declarar sucesso sem resultado verificado.
