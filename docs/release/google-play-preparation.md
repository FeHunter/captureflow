# Preparação para publicação — Google Play Store

**Produto:** Capture Flow (Android nativo)  
**Contato de suporte e privacidade:** captureflowhunter@gmail.com  
**Data da auditoria inicial:** 2026-11-05  
**Modelo definido:** primeiro lançamento público gratuito  
**Status:** preparação técnica em andamento; não publicar ainda.

## 1. Escopo e princípios

- Publicar somente `android-native/`; `legacy-expo/` é histórico.
- Preservar o application ID `com.fehunter.captureflow`.
- Manter arquitetura Feature-first + MVVM e o comportamento atual do produto.
- Fazer mudanças agrupadas, sem commits por arquivo; validar por CI antes de avançar.
- Não gerar nem armazenar chaves privadas no repositório.
- Não alterar permissões/serviços sem mapear uso real e requisitos atuais do Google Play.
- Não publicar nem incrementar versão de produção até aprovação explícita.

## 2. Baseline identificado

| Configuração | Valor atual |
|---|---|
| Módulo | `:app` em `android-native/` |
| Application ID | `com.fehunter.captureflow` |
| Namespace | `com.fehunter.captureflow` |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 26 |
| versionCode / versionName | 5 / 0.3.3 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.20 |
| Java toolchain CI | Temurin 21; bytecode JVM 17 |
| R8 release | Ativo: shrinking, optimization, obfuscation e resource shrinking |
| Edge-to-edge | `enableEdgeToEdge()` na MainActivity; ícones das barras seguem o tema do app |

Não mudar o versionCode/versionName nesta fase. O primeiro lançamento público poderá adotar 1.0.0, mas o versionCode deverá ser maior que qualquer valor já enviado ao Play Console.

## 3. Assinatura e artefatos

O Gradle aceita assinatura release por variáveis de ambiente:

- `ANDROID_KEYSTORE_PATH`: caminho local para o keystore de upload.
- `ANDROID_KEYSTORE_PASSWORD`: senha do keystore.
- `ANDROID_KEY_ALIAS`: alias da chave.
- `ANDROID_KEY_PASSWORD`: senha da chave.

Definir todas as quatro variáveis ou nenhuma. Configuração parcial interrompe a avaliação do build para evitar release acidentalmente mal configurado. Nenhuma senha deve ser gravada em arquivo versionado.

Sem as variáveis, `bundleRelease` valida a compilação e produz bundle não assinado. Esse artefato não está apto para upload. Antes da publicação, configurar a chave de upload e validar assinatura; usar Play App Signing no Play Console e guardar backup seguro da chave de upload. Não compartilhar a chave privada.

A CI executa `bundleRelease` sem segredos para detectar problemas de compilação/configuração. A assinatura e o upload automatizado ficam fora do escopo desta etapa. A release ativa R8 com `isMinifyEnabled = true`, `isShrinkResources = true` e `proguard-android-optimize.txt`; não há regras keep customizadas amplas. O CI confirma a geração de `mapping.txt` e arquiva relatórios R8 de uso/configuração, sem publicar o mapping. Guardar o mapping correspondente a cada AAB publicado em armazenamento privado para retrace de crashes.

## 4. Inventário de dependências, SDKs e tráfego de rede

### 4.1 Dependências Android declaradas

Auditoria do `android-native/app/build.gradle.kts` e `android-native/build.gradle.kts` em 2026-11-05:

| Dependência | Uso observado | Implicação de privacidade |
|---|---|---|
| AndroidX Activity Compose | Integração Activity/Compose | Sem SDK de analytics próprio identificado |
| Jetpack Compose BOM, UI, Material 3 e Material Icons | Interface | Não identificado como coleta de dados |
| CameraX Camera2, Lifecycle e View | Captura escolhida pelo usuário | Acesso à câmera em ação explícita |
| AndroidX Core KTX | APIs Android auxiliares | Sem coleta própria identificada |
| AndroidX ExifInterface | Leitura de metadados de imagem | Processamento local de imagens selecionadas |
| Lifecycle Runtime/ViewModel | Ciclo de vida e estado de UI | Sem coleta própria identificada |
| Room Runtime/KTX + KSP compiler | Banco local SQLite | Dados do caderno persistidos localmente |
| Google ML Kit Text Recognition | OCR | Processamento de texto no fluxo local do app; conferir artefatos transitivos e documentação de entrega do modelo antes do formulário final |
| JUnit e AndroidX Test/Espresso/Compose UI Test | Testes | Dependências de teste; não integram o app release quando corretamente configuradas |

Plugins de build: Android Gradle Plugin 8.13.2, Kotlin 2.2.20 e KSP 2.2.20-2.0.2. Gradle Wrapper 8.13.

Não há dependência direta declarada de Firebase, Google Analytics, Crashlytics, AdMob, SDK de publicidade ou SDK de atribuição no módulo Android examinado. Isso não é uma afirmação sobre toda dependência transitiva nem substitui a análise do AAB final.

### 4.2 Dependências transitivas e auditoria do AAB — 2026-11-05

O inventário foi gerado em CI e o AAB unsigned foi inspecionado. Artefato de auditoria do workflow run 41991366667 (artifact 13398566979): relatório releaseRuntimeClasspath, ML Kit dependencyInsight, manifesto mesclado, manifesto decodificado do AAB, configuração do bundle, inventário ZIP e SHA-256.

Resultados confirmados:
- O AAB contém base/assets/mlkit-google-ocr-models/ e bibliotecas nativas libmlkit_google_ocr_pipeline.so para ARM64, ARM32, x86 e x86_64. O modelo de OCR está empacotado; não depende exclusivamente de download posterior do modelo.
- O classpath inclui com.google.mlkit:text-recognition:16.0.1, com.google.android.gms:play-services-mlkit-text-recognition:19.0.1, com.google.mlkit:common:18.11.0, vision-common, vision-interfaces, firebase-components, firebase-encoders e com.google.android.datatransport (transport-runtime e backend CCT), como dependências transitivas do ML Kit.
- Não foram encontrados firebase-analytics, Crashlytics, AdMob, Google Analytics, SDK de anúncios ou SDK de atribuição no inventário resolvido/AAB. A presença de firebase-components, encoders e transport CCT, isoladamente, não significa Firebase Analytics ou publicidade.
- O manifesto confirma applicationId com.fehunter.captureflow, versionCode 5, versionName 0.3.3, minSdk 26, targetSdk 36, allowBackup=false, permissões de mídia/notificações/FGS e ScreenshotNotificationService com specialUse.
- O AAB de auditoria é unsigned; não é apto para upload na Play Console.

A documentação do ML Kit esclarece que o processamento das imagens/texto e os resultados ocorre no dispositivo e não é enviado aos servidores Google. O SDK pode contactar servidores Google para correções, modelos e compatibilidade de aceleradores e envia métricas técnicas de desempenho/utilização. Para recursos bundled, a documentação lista informações do dispositivo e do app, identificador por instalação, métricas de desempenho, configuração da API, tamanhos de entrada/saída, versão do recurso, tipos de evento e códigos de erro. O Google informa que essas métricas são transmitidas por HTTPS e não são transferidas a terceiros. A política de privacidade e o Data Safety devem declarar esse tratamento técnico.

Fontes oficiais e rascunhos de declaração: docs/release/google-play-submission-drafts.md.

Limite da auditoria: a análise confirma o conteúdo do AAB e dependências resolvidas, mas não substitui testes de rede em runtime em dispositivos nem validação da Play Console. Revalidar o AAB assinado antes do envio.

### 4.3 Tráfego de rede identificado no código Android

| Recurso | Destinatário/serviço | Dados técnicos potencialmente compartilhados |
|---|---|---|
| Importação de links | Site indicado pelo usuário | URL solicitada, cabeçalhos técnicos e endereço IP; recebe HTML limitado para extrair título, descrição e imagens |
| Links do YouTube | Endpoint oficial oEmbed do YouTube | URL do vídeo submetida ao endpoint para obter título e thumbnail |
| Descoberta de feeds | Site indicado pelo usuário | URL pública consultada; recebe HTML para descobrir endpoints RSS/Atom |
| Atualização de feeds | Servidor do feed RSS/Atom | URL do feed, cabeçalhos HTTP condicionais (ETag/Last-Modified) e dados técnicos de conexão |
| Player incorporado | YouTube / youtube-nocookie e recursos associados | Requisições de rede do WebView; player tem JavaScript e DOM Storage habilitados e cookies de terceiros aceitos |
| Navegador/leitor web | Sites acessados pelo usuário | Requisições do site e dados técnicos, sujeitos à política do destino |
| Compartilhamento PDF | Aplicativo destinatário escolhido no Sharesheet | Conteúdo do PDF e arquivos nele incluídos, por ação explícita do usuário |

Não foi identificada no código examinado uma camada própria de analytics ou telemetria. O código do player permite cookies de terceiros, portanto a política não deve sugerir que a interação com YouTube é estritamente sem cookies ou inteiramente local. Sites e serviços de terceiros controlam seus próprios tratamentos de dados e políticas.

### 4.4 Dados armazenados/processados localmente

- Referências, títulos, categorias, subtarefas, anotações, estados e preferências.
- Imagens importadas e arquivos associados, em armazenamento privado do aplicativo.
- OCR de imagens selecionadas, executado no dispositivo.
- Metadados do MediaStore (identificador, nome, data e caminho relativo) consultados pelo monitoramento de screenshots.
- URI/estado de deduplicação e watermark do serviço, em preferências locais.
- Arquivo ZIP de backup, gerado apenas quando o usuário solicita exportação; fica no destino escolhido pelo usuário.

O monitoramento não lê pixels durante a varredura de descoberta, não captura a tela, não usa Accessibility Services e não envia imagens aos servidores próprios do CaptureFlow. A importação exige revisão e confirmação do usuário.

### 4.5 Conclusão provisória para Data Safety

- Não afirmar que o aplicativo é totalmente offline: existem requisições a sites, feeds, YouTube e WebView.
- Dados do caderno, imagens, metadados de screenshots e texto OCR permanecem locais no fluxo implementado; não foram identificados uploads desses conteúdos para servidores próprios do CaptureFlow.
- O SDK ML Kit bundled envia métricas técnicas de uso/diagnóstico ao Google, conforme documentado em google-play-submission-drafts.md; incluir as categorias aplicáveis no formulário.
- Não foram encontrados SDKs de publicidade/analytics/Crashlytics no classpath resolvido e no AAB auditado. Confirmar novamente no AAB assinado e manter a declaração de anúncios coerente.
- Separar dados coletados pelo SDK, dados acessados localmente pelo app, requisições feitas a sites escolhidos pelo usuário e conteúdo transferido pelo Sharesheet.
- A proposta de Data Safety é preliminar: a classificação final deve ser confirmada no formulário da Play Console e após testes de rede/runtime.

## 5. Manifest e permissões — monitoramento contínuo opt-in

A decisão de produto é oferecer detecção contínua de screenshots enquanto o usuário mantiver a função ativada. O monitoramento é executado por um Foreground Service do tipo `specialUse`, com notificação persistente e ação explícita para interromper.

Permissões relacionadas:

- INTERNET — requisições de metadados, RSS/Atom, YouTube e integrações web.
- CAMERA — captura de imagens dentro do aplicativo.
- READ_MEDIA_IMAGES — consulta aos metadados do MediaStore para identificar screenshots.
- READ_MEDIA_VISUAL_USER_SELECTED — identifica o modo de acesso limitado do Android 14+; acesso parcial não habilita o monitoramento contínuo.
- READ_EXTERNAL_STORAGE com maxSdkVersion 32 — permissão equivalente para versões Android anteriores ao Android 13.
- POST_NOTIFICATIONS — notificação persistente e alertas de screenshots no Android 13 ou superior.
- FOREGROUND_SERVICE e FOREGROUND_SERVICE_SPECIAL_USE — execução do serviço em primeiro plano.

O Manifest declara `ScreenshotNotificationService` com `foregroundServiceType="specialUse"` e propriedade `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. Não há inicialização automática no boot.

### Funcionamento técnico

O usuário ativa o recurso pela interface, após disclosure e concessão das permissões. O serviço inicia a notificação foreground imediatamente, registra um ContentObserver no MediaStore e consulta apenas metadados (ID, nome, data e caminho relativo quando disponível). A busca usa um watermark incremental por `DATE_ADDED`, uma janela de tolerância para gravações atrasadas e deduplicação de URIs para reduzir alertas repetidos. A consulta é limitada a um conjunto recente de registros; não lê pixels, não captura a tela e não utiliza Accessibility Services, overlay ou APIs de gravação de tela.

Cada screenshot identificada gera uma notificação individual. Ao tocá-la, o usuário abre uma prévia dentro do CaptureFlow, escolhe uma categoria e confirma a importação. A imagem original permanece na galeria. A notificação persistente apresenta a ação **Desativar**, e o cabeçalho do app também permite interromper o serviço. `START_STICKY` permite ao Android tentar recriar o serviço após encerramento do processo; a retomada respeita a preferência de ativação e as permissões. O app não registra receiver de boot e não reativa o recurso após reinicialização do dispositivo.

No Android 14+, a permissão `READ_MEDIA_VISUAL_USER_SELECTED` permite identificar acesso limitado. Como o recurso precisa observar novas screenshots fora de uma seleção prévia, o serviço só inicia quando `READ_MEDIA_IMAGES` está integralmente concedida. A seleção manual continua disponível pelo Photo Picker.

### Justificativa para Google Play e risco de elegibilidade

A justificativa proposta para `specialUse` é: o usuário ativa explicitamente uma função de captura de referências; o serviço observa continuamente o MediaStore para reconhecer novas screenshots enquanto o usuário usa outros aplicativos e apresenta uma notificação que permite revisar e importar a captura. A notificação persistente mantém o usuário informado e oferece parada imediata.

Essa justificativa descreve o comportamento, mas não garante aprovação. A política também exige que um Foreground Service execute apenas pelo tempo necessário para concluir sua tarefa. Um monitoramento sem prazo de término tem risco elevado de ser considerado incompatível com esse critério, além do risco de o Google Play entender que o caso não é elegível como funcionalidade central ou que `specialUse` não é apropriado. A submissão deve incluir a declaração de Foreground Service, descrição do subtipo, impacto de adiamento/interrupção e vídeo demonstrativo do fluxo real. A descrição deve explicar honestamente que, se o serviço for interrompido, a detecção pode pausar e capturas desse intervalo podem não gerar alertas. Não declarar comportamento que não esteja implementado.

Se houver rejeição, registrar o motivo exato da revisão e avaliar primeiro a correção solicitada. Como alternativa de menor risco, remover o serviço contínuo e manter Photo Picker para seleção manual; não remover funcionalidades nem permissões sem revisar o impacto e a decisão do produto.

## 6. Backup e dados locais

O Manifest atual usa `android:allowBackup="false"`, com fullBackupContent e dataExtractionRules configurados para excluir os domínios do app do backup em nuvem e da transferência entre dispositivos. A única forma suportada de backup/restauração é a exportação/importação manual em ZIP, iniciada pelo usuário. Manter a tela de privacidade e a documentação alinhadas com essa política.

## 7. Política de privacidade e Data Safety

**Contato de suporte e privacidade:** captureflowhunter@gmail.com

A política deve estar disponível em página HTML pública, não editável, acessível mundialmente e sem login, e também deve ser acessível dentro do aplicativo. A URL definitiva deverá ser cadastrada na Play Console.

A versão final deve cobrir:
- dados locais e permissões usadas;
- OCR local e eventual comportamento de download do modelo;
- monitoramento de screenshots e metadados consultados;
- requisições para sites, feeds, YouTube, player incorporado e leitor web;
- cookies/armazenamento WebView e terceiros;
- compartilhamento de PDF por escolha explícita;
- backup manual, retenção e exclusão;
- contato do desenvolvedor e canal para solicitações.

Pendências:
- Publicar a página de política no domínio do portfólio.
- Atualizar a tela Privacidade no app se a auditoria revelar conteúdo ausente ou impreciso.
- [x] Concluir inventário de dependências transitivas e inspeção do AAB unsigned.
- [ ] Reinspecionar o AAB assinado e validar comportamento de rede/runtime.
- [ ] Atualizar política publicada com métricas técnicas do ML Kit.
- [ ] Preencher Data Safety com base no inventário e nas ressalvas do rascunho.
- [x] Não foram encontrados SDKs de anúncios no classpath transitivo/AAB auditado; confirmar no AAB assinado.

## 8. Cadastro da loja — pendente

- Criar conta Play Console e concluir verificação de identidade/contato.
- Confirmar tipo de conta e eventuais requisitos de teste fechado aplicáveis à conta pessoal.
- Definir nome, descrição curta/longa, categoria, idioma e contato de suporte.
- Preparar ícone 512 × 512, feature graphic 1024 × 500 e screenshots reais do app.
- Preencher classificação de conteúdo, público-alvo, anúncios, Data Safety e acesso ao app.
- Preparar faixa de teste interno, depois teste fechado e lançamento gradual.

## 9. Testes de release

- CI: `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `bundleRelease`.
- Teste manual em API 26 e versões recentes, incluindo API 35 e 36.
- Testar edge-to-edge: status/navigation bars, tema claro/escuro (inclusive diferente do tema do sistema), gestos e navegação de três botões, display cutouts, teclado/IME, dialogs, fullscreen, predictive back, orientação, telas grandes e fontes ampliadas.
- Conferir no App Bundle Explorer do Play Console os percentuais separados de shrinking, obfuscation e optimization do AAB efetivamente enviado; não inferir os percentuais apenas pelo sucesso do R8 no CI.
- Testar Photo Picker, câmera, OCR, compartilhamento PDF via FileProvider, backup/restauração, RSS, player e screenshot monitoring opt-in.
- Testar permissões negadas/revogadas e comportamento sem conexão.
- Instalar AAB assinado via faixa interna do Play Console e validar atualização sobre instalação anterior.
- Avaliar compatibilidade de bibliotecas nativas com páginas de memória de 16 KB.

## 10. Critérios de saída desta preparação técnica

- [x] Dependências diretas Android e fluxos de rede revisados estaticamente.
- [ ] Dependências transitivas e AAB release inspecionados.
- [ ] Build release/AAB compila no CI.
- [ ] Assinatura release validada com chave de upload fora do Git.
- [ ] Inventário e justificativa de permissões concluídos.
- [x] Justificativas preliminares para READ_MEDIA_IMAGES e FGS specialUse documentadas; elegibilidade ainda sujeita à análise da Play.
- [x] Auto Backup e transferência automática desabilitados; regras explícitas e documentação adicionadas.
- [ ] Política de privacidade publicada e coerente com Data Safety.
- [ ] Testes funcionais e de compatibilidade aprovados.
- [ ] Materiais da loja prontos.
- [ ] Conta Play Console verificada e requisitos de teste cumpridos.

**Não considerar o app pronto para publicação enquanto qualquer item crítico acima estiver pendente.**


## 11. Rascunhos de submissão e próximos passos

Consultar docs/release/google-play-submission-drafts.md para o rascunho de Data Safety, justificativas de permissões, texto do Foreground Service, roteiro de vídeo, instruções locais de assinatura e checklist de testes reais. Esses textos são material de preparação, não declarações já enviadas ou aprovadas pela Google.
