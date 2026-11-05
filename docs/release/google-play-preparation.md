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

## 4. Manifest e permissões — funcionalidades mantidas

A decisão de produto é manter o monitoramento opt-in de screenshots no primeiro lançamento. As permissões abaixo fazem parte de funcionalidades existentes e não serão removidas apenas para simplificar a submissão:

- INTERNET — requisições de metadados, RSS/Atom, YouTube e integrações web.
- CAMERA — captura de imagens dentro do aplicativo.
- POST_NOTIFICATIONS — notificações de screenshots e visibilidade do serviço.
- FOREGROUND_SERVICE e FOREGROUND_SERVICE_SPECIAL_USE — serviço contínuo, iniciado pelo usuário, para observar novas imagens e apresentar uma ação de importação.
- READ_MEDIA_IMAGES — consulta ao MediaStore para detectar screenshots recentes.
- READ_EXTERNAL_STORAGE com maxSdkVersion 32 — permissão equivalente para versões Android anteriores ao Android 13.

### Justificativa funcional para a declaração Play Console

O CaptureFlow é um caderno de referências visuais. A detecção de screenshots permite que o usuário capture conteúdo em outros aplicativos e receba uma notificação para importar essa imagem como referência, sem precisar interromper o fluxo de uso, abrir manualmente a galeria e localizar o arquivo. O acesso à coleção de imagens é utilizado para consultar metadados (nome, caminho e data) e identificar capturas recentes; o serviço não captura a tela, não usa Accessibility Services e não monitora o conteúdo visual de outros aplicativos. A imagem é processada/importada somente quando o usuário escolhe a ação oferecida. O recurso é opcional, exige ativação e permissões explícitas, mantém notificação persistente enquanto ativo e pode ser interrompido a qualquer momento nas opções do app ou pelas configurações Android.

O Photo Picker permanece como alternativa para seleção manual de imagens, mas não oferece monitoramento de novas screenshots em segundo plano e, portanto, não substitui o acesso ao MediaStore necessário para essa funcionalidade.

### Ações obrigatórias antes do release

1. Preparar declaração de acesso a fotos/vídeos explicando a funcionalidade principal, o fluxo de detecção e por que o Photo Picker não substitui a consulta contínua.
2. Preparar declaração de foreground service `specialUse`, incluindo subtipo, início explícito pelo usuário, notificação persistente, forma de interrupção e vídeo demonstrativo se solicitado.
3. Confirmar elegibilidade e requisitos vigentes do Google Play antes de submeter. A justificativa não garante aprovação; responder a eventuais solicitações de esclarecimento e estar preparado para rever o fluxo caso o Google não aceite a exceção.
4. Testar concessão, recusa, revogação e comportamento após reinício/atualização.
5. Manter a explicação visível na tela Privacidade e tornar a desativação fácil de encontrar no app.

## 5. Backup e dados locais

O Manifest atual usa `android:allowBackup="true"`. Revisar regras Android Auto Backup e transferência de dispositivo para Room, arquivos privados e imagens. Comparar com o backup ZIP manual do app, que é aditivo e controlado pelo usuário. Decidir explicitamente o que deve ou não migrar; documentar a experiência. Não desabilitar backup por conveniência sem avaliar impacto de troca de aparelho.

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
