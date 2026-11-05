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

## 4. Manifest e permissões — detecção em primeiro plano

A decisão de produto é manter a detecção opt-in de screenshots, limitada ao período em que a MainActivity está em primeiro plano (onResume até onPause). Não existe serviço em segundo plano para essa funcionalidade.

Permissões mantidas:

- INTERNET — requisições de metadados, RSS/Atom, YouTube e integrações web.
- CAMERA — captura de imagens dentro do aplicativo.
- READ_MEDIA_IMAGES — consulta ao MediaStore para identificar screenshots recentes enquanto o app está em primeiro plano.
- READ_EXTERNAL_STORAGE com maxSdkVersion 32 — permissão equivalente para versões Android anteriores ao Android 13.

Removidas para esse fluxo:

- POST_NOTIFICATIONS — não são emitidas notificações de screenshot.
- FOREGROUND_SERVICE e FOREGROUND_SERVICE_SPECIAL_USE — não existe serviço foreground de monitoramento.
- Declaração de ScreenshotNotificationService e propriedade PROPERTY_SPECIAL_USE_FGS_SUBTYPE — removidas do Manifest.

### Funcionamento técnico

Quando o usuário ativa a detecção e concede acesso a imagens, ScreenshotMonitor registra um ContentObserver no MediaStore durante o período em que a Activity está resumed. A consulta limita os resultados a imagens adicionadas desde o início da sessão em primeiro plano, inspeciona metadados (nome, caminho relativo e data) e procura padrões conhecidos de screenshots. Ao detectar uma captura, o app apresenta uma prévia dentro da própria interface. A imagem só é importada após confirmação explícita do usuário.

Em onPause o observer e tarefas pendentes são encerrados. Ao retornar ao primeiro plano, se a preferência continuar ativa e a permissão estiver concedida, o monitor é iniciado novamente. Se a permissão for revogada, a preferência de detecção é desativada. O fluxo não captura a tela, não usa Accessibility Services, não lê pixels durante a varredura e não notifica enquanto o app está em segundo plano.

### Photo Picker e limite de elegibilidade

O Photo Picker continua sendo o caminho de seleção manual e não exige acesso amplo à biblioteca. Ele não oferece uma API para observar automaticamente novas screenshots adicionadas ao MediaStore, mesmo quando o CaptureFlow está aberto. Por isso, não substitui tecnicamente o observador usado para detecção automática em primeiro plano.

Essa diferença técnica não garante que o Google Play aceite READ_MEDIA_IMAGES: a política também avalia se o acesso amplo é necessário para uma funcionalidade central do app. O CaptureFlow é um caderno de referências e continua funcional sem a detecção automática. A submissão deve explicar com transparência que a detecção é uma conveniência opt-in, limitada ao primeiro plano, e manter o Photo Picker como alternativa manual. Se o Google considerar que o acesso amplo não é elegível, será necessário remover a detecção automática e depender da seleção manual.

### Ações antes do release

1. Confirmar em dispositivo Android real que o observer inicia em onResume e é encerrado em onPause, inclusive ao abrir outra Activity/app e ao exibir a tela de permissões.
2. Confirmar que a explicação destacada aparece antes do diálogo de permissão, que Continuar solicita a permissão e Agora não não solicita acesso nem bloqueia o Photo Picker.
3. Testar permissão concedida, negada, revogada e acesso parcial em versões Android compatíveis.
4. Demonstrar que a prévia aparece somente enquanto o app está aberto e que nada é importado sem confirmação.
5. Atualizar a declaração Play Console para descrever detecção somente em primeiro plano; não declarar foreground service nem specialUse.
6. Confirmar a elegibilidade de READ_MEDIA_IMAGES com a política vigente. A justificativa não garante aprovação; se rejeitada, remover a detecção automática e manter Photo Picker.

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
