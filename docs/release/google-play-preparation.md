# Preparação para publicação — Google Play Store

**Produto:** Capture Flow (Android nativo)  
**Data da auditoria inicial:** 2026-11-05  
**Branch de trabalho:** `chore/google-play-preparation`  
**Modelo definido:** primeiro lançamento público gratuito  
**Conta Play Console:** ainda não criada  
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

Não mudar o versionCode/versionName nesta fase. O primeiro lançamento público poderá adotar 1.0.0, mas o versionCode deverá ser maior que qualquer valor já enviado ao Play Console.

## 3. Assinatura e artefatos

O Gradle aceita assinatura release por variáveis de ambiente:

- `ANDROID_KEYSTORE_PATH`: caminho local para o keystore de upload.
- `ANDROID_KEYSTORE_PASSWORD`: senha do keystore.
- `ANDROID_KEY_ALIAS`: alias da chave.
- `ANDROID_KEY_PASSWORD`: senha da chave.

Definir todas as quatro variáveis ou nenhuma. Configuração parcial interrompe a avaliação do build para evitar release acidentalmente mal configurado. Nenhuma senha deve ser gravada em arquivo versionado.

Sem as variáveis, `bundleRelease` valida a compilação e produz bundle não assinado. Esse artefato não está apto para upload. Antes da publicação, configurar a chave de upload e validar assinatura; usar Play App Signing no Play Console e guardar backup seguro da chave de upload. Não compartilhar a chave privada.

A CI executa `bundleRelease` sem segredos para detectar problemas de compilação/configuração. A assinatura e o upload automatizado ficam fora do escopo desta etapa.

## 4. Manifest e permissões — monitoramento contínuo opt-in

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

Essa justificativa descreve o comportamento, mas não garante aprovação. O Google Play pode entender que monitoramento contínuo de screenshots não é elegível como funcionalidade central ou que `specialUse` não é apropriado. A submissão deve incluir a declaração de Foreground Service, descrição do subtipo, impacto de interrupção/adiamento e vídeo demonstrativo do fluxo real. Não declarar comportamento que não esteja implementado.

Se houver rejeição, registrar o motivo exato da revisão e avaliar primeiro a correção solicitada. Como alternativa de menor risco, remover o serviço contínuo e manter Photo Picker para seleção manual; não remover funcionalidades nem permissões sem revisar o impacto e a decisão do produto.

### Ações antes do release

1. Testar início iniciado pela interface, notificação persistente, ação Desativar e parada pelo cabeçalho.
2. Testar Android 13+ com permissão de notificações concedida, negada e revogada.
3. Testar READ_MEDIA_IMAGES concedida, negada, acesso limitado e revogada; manter Photo Picker funcional sem acesso amplo.
4. Testar screenshots criadas em outros apps, várias capturas em sequência, duplicação, MediaStore lento e reinício do processo.
5. Confirmar que o serviço não inicia após reboot e não importa/apaga a imagem sem confirmação.
6. Validar a declaração `specialUse`, a justificativa, o impacto de interrupção/adiamento e o vídeo exigidos no Play Console.
7. Revisar a elegibilidade de READ_MEDIA_IMAGES separadamente da elegibilidade de `specialUse`.

## 5. Backup e dados locais

O Manifest atual usa `android:allowBackup="false"`, com fullBackupContent e dataExtractionRules configurados para excluir os domínios do app do backup em nuvem e da transferência entre dispositivos. A única forma suportada de backup/restauração é a exportação/importação manual em ZIP, iniciada pelo usuário. Manter a tela de privacidade e a documentação alinhadas a essa política.

## 6. Privacidade e Data Safety

A declaração deverá refletir o app real, inclusive integrações de terceiros. O app não possui conta nem sincronização própria em nuvem e processa OCR localmente, mas pode realizar requisições externas para metadados, RSS/Atom, YouTube e conteúdo web; o usuário também pode compartilhar documentos com outros apps.

Pendências:

- Publicar política de privacidade em URL pública, com contato de suporte.
- Conferir texto da tela Privacidade contra comportamento atual.
- Inventariar bibliotecas/SDKs e seus dados/processamentos.
- Preencher Data Safety com base no inventário completo, não presumir que tudo é local.
- Definir processo de exclusão/solicitações de privacidade e canal de contato.
- Declarar anúncios corretamente (modelo inicial definido como gratuito; não implica presença de anúncios).

## 7. Testes de release

- CI: `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `bundleRelease`.
- Teste manual em API 26 e versões recentes, incluindo API 36.
- Testar edge-to-edge, barras do sistema, predictive back, orientação, telas grandes e fontes ampliadas.
- Testar Photo Picker, câmera, OCR, compartilhamento PDF via FileProvider, backup/restauração, RSS, player e screenshot monitoring opt-in.
- Testar permissões negadas/revogadas e comportamento sem conexão.
- Instalar AAB assinado via faixa interna do Play Console e validar atualização sobre instalação anterior.
- Avaliar compatibilidade de bibliotecas nativas com páginas de memória de 16 KB.

## 8. Cadastro da loja — pendente

- Criar conta Play Console e concluir verificação de identidade/contato.
- Confirmar tipo de conta e eventuais requisitos de teste fechado aplicáveis à conta pessoal.
- Definir nome, descrição curta/longa, categoria, idioma e contato de suporte.
- Preparar ícone 512 × 512, feature graphic 1024 × 500 e screenshots reais do app.
- Preencher classificação de conteúdo, público-alvo, anúncios, Data Safety e acesso ao app.
- Preparar faixa de teste interno, depois teste fechado e lançamento gradual.

## 9. Critérios de saída desta preparação técnica

- [ ] Build release/AAB compila no CI.
- [ ] Assinatura release validada com chave de upload fora do Git.
- [ ] Inventário e justificativa de permissões concluídos.
- [x] Auto Backup e transferência automática desabilitados; regras explícitas e documentação adicionadas.
- [ ] Política de privacidade publicada e coerente com Data Safety.
- [ ] Testes funcionais e de compatibilidade aprovados.
- [ ] Materiais da loja prontos.
- [ ] Conta Play Console verificada e requisitos de teste cumpridos.

**Não considerar o app pronto para publicação enquanto qualquer item crítico acima estiver pendente.**
