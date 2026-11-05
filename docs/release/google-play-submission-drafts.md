# Google Play — rascunhos de declarações e validação

**Produto:** Capture Flow  
**Application ID:** com.fehunter.captureflow  
**Baseline auditado:** AAB unsigned, versionCode 5 / versionName 0.3.3  
**Status:** material preparatório; não copiar para a Play Console sem revisar o formulário vigente e o AAB assinado.

> Este documento descreve o comportamento observado e separa fatos confirmados de classificações propostas. Não garante elegibilidade ou aprovação. Em caso de divergência, prevalecem o código do release submetido e as instruções atuais da Play Console.

## 1. Data Safety — inventário e proposta preliminar

### 1.1 O que permanece no dispositivo

| Dado | Origem/acesso | Uso no CaptureFlow | Transmitido pelo CaptureFlow? |
|---|---|---|---|
| Referências, títulos, categorias, subtarefas, notas e estados | Inseridos/importados pelo usuário | Organizar o caderno | Não, permanecem no banco local |
| Imagens escolhidas, fotografadas ou importadas | Câmera, Sharesheet, Photo Picker ou screenshot revisada | Capa/anexo da referência | Não para servidores próprios; podem ser transferidas se o usuário exportar/compartilhar |
| Texto reconhecido por OCR | Imagens selecionadas/importadas | Sugerir título/metadados | Não; reconhecimento ocorre no dispositivo |
| Metadados de imagens no MediaStore | ID, nome, data e caminho relativo; somente quando monitoramento está ativo | Detectar possíveis screenshots recentes | Não; consulta local |
| Watermark e URIs deduplicadas | Serviço de monitoramento | Evitar alertas repetidos | Não; preferências locais |
| Backup ZIP | Gerado sob solicitação explícita | Exportação/restauração manual | Não é enviado pelo app; o usuário escolhe onde salvar/compartilhar |

Acesso local a fotos/câmera não significa, por si só, que esses dados sejam coletados/transmitidos para o desenvolvedor. Entretanto, fotos são dados pessoais e sensíveis sob as políticas do Google Play; a permissão ampla e o propósito de acesso devem ser declarados e justificados separadamente.

### 1.2 Dados técnicos do Google ML Kit

O AAB auditado contém os modelos bundled do OCR em base/assets/mlkit-google-ocr-models/ e bibliotecas nativas do pipeline. O SDK processa a imagem e produz o texto no dispositivo; a imagem de entrada e o resultado do OCR não são enviados aos servidores Google segundo os termos de privacidade do ML Kit.

O ML Kit pode comunicar-se com servidores Google para correções, informações de compatibilidade de aceleradores e atualizações relacionadas aos SDKs. Também envia métricas de desempenho e utilização. Para as features bundled, a documentação lista:

- Informações do dispositivo (fabricante, modelo, versão/build do sistema e aceleradores ML disponíveis).
- Informações do aplicativo (nome do pacote e versões).
- Identificador por instalação (não destinado a identificar unicamente pessoa ou dispositivo físico).
- Métricas de desempenho, como latência.
- Configuração da API, como formato e resolução da imagem.
- Tamanho das entradas e saídas.
- Versão da feature.
- Tipos de evento (inicialização, download de modelo quando aplicável, detecção e liberação de recursos).
- Códigos de erro dos eventos.

O Google declara que as métricas listadas são transmitidas por HTTPS e não são transferidas a terceiros. O tratamento é para diagnóstico, análise de uso, manutenção e melhoria do SDK e detecção de abuso.

**Classificação sugerida no formulário (confirmar nomes/categorias exibidos pela Play Console):**

| Categoria do formulário | Tipo de dado candidato | Motivo |
|---|---|---|
| App info and performance | Diagnostics / Other app performance data | Métricas de latência, versão da feature, erros e eventos técnicos |
| Device or other IDs | Device or other IDs | Identificador por instalação usado para diagnóstico/uso |
| App activity | App interactions — avaliar com cuidado | Eventos técnicos de inicialização/detecção podem ser considerados eventos de uso; usar a classificação indicada pela definição vigente da Play Console |

**Finalidades candidatas:** Analytics (métricas de utilização) e App functionality / diagnostics (manutenção, depuração e melhoria do SDK), conforme opções e definições vigentes do formulário.

**Compartilhamento:** a documentação do ML Kit diz que os dados técnicos listados não são transferidos a terceiros. Não marcar como compartilhados apenas pela existência de transporte HTTPS para Google; confirmar a definição de “service provider” e as perguntas específicas da Play Console.

**Criptografia:** Google declara HTTPS para os dados técnicos coletados pelo ML Kit. Não extrapolar essa afirmação para dados enviados a sites externos ou para armazenamento local.

**Exclusão:** não prometer mecanismo de exclusão individual de métricas do ML Kit sem documentação específica. Não declarar que o usuário pode excluir identificadores técnicos enviados pelo SDK.

Fonte oficial: https://developers.google.com/ml-kit/android-data-disclosure  
Termos e privacidade ML Kit: https://developers.google.com/ml-kit/terms

### 1.3 Conexões com sites e serviços externos

O aplicativo não possui backend próprio, conta, sincronização em nuvem ou camada própria de analytics identificada. Ainda assim, durante ações escolhidas pelo usuário, o dispositivo faz requisições para:

- URL compartilhada para extração de título/descrição/imagens;
- endpoint oEmbed do YouTube para metadados de vídeo;
- sites e URLs de feeds RSS/Atom;
- YouTube e recursos associados no WebView/player;
- sites abertos no leitor web/navegador.

Os servidores de destino recebem requisições de rede e podem receber endereço IP, URL consultada, cabeçalhos e dados técnicos próprios do protocolo. O CaptureFlow não controla as práticas desses provedores. O conteúdo do Sharesheet/PDF é transferido ao app destinatário somente após ação explícita do usuário.

Não marcar “nenhuma coleta” de forma automática. Ao preencher Data Safety, revisar as perguntas específicas sobre dados enviados a terceiros, transferências iniciadas pelo usuário e dados processados por SDKs. A política de privacidade descreve esses fluxos separadamente.

### 1.4 Respostas que não devem ser afirmadas sem nova evidência

- “O app é totalmente offline.”
- “Nenhum dado sai do dispositivo.”
- “O Google não recebe nenhum dado.”
- “O OCR não transmite absolutamente nada”: a imagem e o texto de OCR não são enviados, mas métricas técnicas do SDK são.
- “Não há coleta por SDKs”: ML Kit envia métricas técnicas.
- “Não há anúncios” sem confirmar novamente o AAB assinado e o cadastro de monetização na Play Console.

## 2. READ_MEDIA_IMAGES — justificativa proposta

### 2.1 Texto para a declaração de fotos/vídeos

> O CaptureFlow é um caderno pessoal de referências. Além da seleção manual de imagens pelo Android Photo Picker, o usuário pode ativar voluntariamente um recurso de detecção de novas screenshots. Quando ativado, o aplicativo observa alterações no MediaStore e consulta metadados limitados — identificador, nome, data e pasta relativa — para reconhecer novas imagens que aparentam ser capturas de tela, mesmo quando o usuário está utilizando outros aplicativos.
>
> O Android Photo Picker concede acesso apenas às imagens que o usuário seleciona em uma sessão. Ele não fornece ao aplicativo um fluxo contínuo de eventos sobre novas screenshots criadas posteriormente em outros aplicativos e não permite monitorar a biblioteca para gerar alertas automáticos. Por isso, o Picker continua sendo a alternativa para importação manual, mas não substitui tecnicamente o monitoramento opt-in.
>
> O CaptureFlow não lê pixels durante a varredura de descoberta, não captura a tela de outros aplicativos e não utiliza Accessibility Services. Uma imagem detectada não é importada automaticamente: o usuário abre a notificação, revisa a prévia, escolhe uma categoria e confirma. A imagem original permanece na galeria. O serviço pode ser desativado a qualquer momento e, sem a permissão ampla, a seleção manual pelo Photo Picker continua disponível.

### 2.2 Limite de elegibilidade — risco alto

A justificativa acima demonstra a diferença técnica entre Picker e observação contínua, mas **não prova que o acesso amplo seja elegível**. A política exige que READ_MEDIA_IMAGES seja necessário para a funcionalidade central promovida no Google Play. O monitoramento é opcional e o CaptureFlow continua funcionando com importação manual, o que pode enfraquecer a alegação de que a permissão é indispensável à função central.

Não ocultar que a permissão permite acesso amplo a imagens. Não descrever o app como galeria ou gerenciador de fotos se ele não for isso. Se a Play Console concluir que o recurso não justifica a permissão ampla, a alternativa de conformidade é retirar o monitoramento contínuo e manter Photo Picker/Compartilhamento nativo.

### 2.3 Disclosure no app antes da permissão

Texto sugerido (validar se coincide com a tela real):

> **Monitoramento de screenshots**
>
> Se você ativar este recurso, o CaptureFlow acompanhará novas imagens no MediaStore para identificar possíveis screenshots enquanto você usa outros aplicativos. Isso exige acesso amplo às imagens do dispositivo e uma notificação persistente. Durante a identificação, o app consulta metadados como nome, data e pasta; não captura sua tela nem envia imagens aos servidores do CaptureFlow. Nenhuma imagem é salva como referência sem sua revisão e confirmação. Você pode desativar o monitoramento a qualquer momento. Se não conceder o acesso, ainda poderá importar imagens manualmente pelo seletor do Android.

## 3. Foreground Service — declaração specialUse

### 3.1 Subtipo declarado no manifesto

O manifesto auditado contém android:foregroundServiceType="specialUse", FOREGROUND_SERVICE_SPECIAL_USE e PROPERTY_SPECIAL_USE_FGS_SUBTYPE.

Valor atual:

> User-enabled continuous screenshot monitoring for the user's personal reference notebook. The service observes new screenshot metadata in MediaStore and offers an explicit import notification.

### 3.2 Texto proposto para a declaração Play Console

> O serviço é iniciado somente após o usuário ativar o monitoramento de screenshots na interface do CaptureFlow e conceder as permissões necessárias. Ele permanece visível por meio de uma notificação persistente com ação “Desativar”. O serviço registra um ContentObserver no MediaStore e consulta metadados de imagens recentes para reconhecer nomes e caminhos associados a screenshots. Não captura a tela, não lê pixels de outros aplicativos durante a descoberta, não usa Accessibility Services e não envia imagens a servidores próprios.
>
> Quando uma possível screenshot é detectada, o serviço apresenta uma notificação individual. O usuário abre a prévia, escolhe a categoria e confirma a importação. A imagem original permanece na galeria. O serviço não é iniciado no boot e pode ser encerrado pela notificação ou pela interface do aplicativo.
>
> Se o serviço for interrompido pelo sistema, o monitoramento pode pausar e screenshots criadas durante a interrupção podem não gerar alertas. O recurso não é necessário para a seleção manual de imagens, que continua disponível pelo Photo Picker.

### 3.3 Avaliação crítica antes do envio

O caso não corresponde a um tipo de FGS padrão e specialUse é residual, sujeito a revisão. Além disso, a política atual exige que o FGS execute apenas pelo tempo necessário para concluir a tarefa. O serviço implementado é contínuo e não tem uma condição natural de conclusão enquanto o usuário mantém a função ligada.

Portanto, existe **risco elevado de rejeição** mesmo com consentimento explícito, notificação persistente, ação de parada e declaração tecnicamente correta. Não afirmar que o serviço atende a todos os critérios. O formulário deve responder honestamente como a atividade é iniciada, percebida, interrompida e por que a interrupção prejudica a experiência. Não usar specialUse como forma de contornar uma regra de duração.

Fontes:
- https://support.google.com/googleplay/android-developer/answer/16559646
- https://developer.android.com/develop/background-work/services/fgs/service-types

## 4. Vídeo demonstrativo para a revisão

Gravar a tela de um dispositivo real, sem cortes que escondam permissões ou estados relevantes. Duração sugerida: 45–75 segundos.

1. Mostrar a versão do CaptureFlow e a tela onde o usuário encontra o monitoramento.
2. Abrir o disclosure completo antes da solicitação de permissões.
3. Mostrar a concessão de acesso completo a imagens e notificações no Android.
4. Ativar o monitoramento por ação explícita do usuário.
5. Mostrar a notificação persistente “CaptureFlow · Monitoramento ativo” e a ação “Desativar”.
6. Sair do CaptureFlow e criar uma screenshot em outro aplicativo.
7. Mostrar a notificação individual “Nova screenshot detectada”.
8. Abrir a prévia, escolher uma categoria e confirmar a importação.
9. Voltar à biblioteca e mostrar a referência criada.
10. Mostrar que a imagem original continua na galeria e encerrar o serviço pela ação “Desativar”.

Não simular notificações nem acelerar artificialmente a detecção. Se algum passo não funcionar no dispositivo/versão Android usada, corrigir o app ou atualizar a descrição; não editar o vídeo para sugerir comportamento inexistente.

## 5. Chave de upload e AAB assinado

### 5.1 Regras de segurança

- Gerar a chave no computador do desenvolvedor; nunca em CI público ou no chat.
- Guardar o arquivo .jks fora do repositório e fora de pastas sincronizadas publicamente.
- Guardar uma cópia de segurança criptografada em local seguro.
- Nunca commitar keystore, senhas, propriedades locais ou certificados privados.
- Ativar Play App Signing no Play Console; a chave local será a upload key.
- Não enviar o keystore nem senhas para o assistente.

### 5.2 PowerShell — gerar a chave localmente

Criar uma pasta privada, por exemplo:

    $keyDir = "$env:USERPROFILE\Android\keys"
    New-Item -ItemType Directory -Force $keyDir
    keytool -genkeypair -v -keystore "$keyDir\captureflow-upload.jks" -keyalg RSA -keysize 4096 -validity 10000 -alias captureflow-upload

O keytool solicitará as senhas interativamente. Não as inclua na linha de comando, no histórico do terminal ou em arquivos versionados. Preencha os dados do certificado de forma consistente e guarde alias/senhas com o keystore.

### 5.3 Gerar o bundle assinado

A partir de android-native/ no PowerShell:

    $env:ANDROID_KEYSTORE_PATH = "$env:USERPROFILE\Android\keys\captureflow-upload.jks"
    $env:ANDROID_KEYSTORE_PASSWORD = Read-Host "Senha do keystore"
    $env:ANDROID_KEY_ALIAS = "captureflow-upload"
    $env:ANDROID_KEY_PASSWORD = Read-Host "Senha da chave"
    .\gradlew.bat :app:bundleRelease

O artefato esperado é:

    app/build/outputs/bundle/release/app-release.aab

Após o build, limpar as variáveis da sessão:

    Remove-Item Env:ANDROID_KEYSTORE_PATH
    Remove-Item Env:ANDROID_KEYSTORE_PASSWORD
    Remove-Item Env:ANDROID_KEY_ALIAS
    Remove-Item Env:ANDROID_KEY_PASSWORD

Não alterar versionCode/versionName nesta preparação. Antes do primeiro upload, confirmar no Play Console qual versionCode está livre. A versão 5 / 0.3.3 é o baseline do AAB auditado, não uma decisão final de release.

### 5.4 Verificações locais

    keytool -list -v -keystore "$env:USERPROFILE\Android\keys\captureflow-upload.jks" -alias captureflow-upload
    jarsigner -verify -verbose -certs .\app\build\outputs\bundle\release\app-release.aab

Também executar bundletool validate --bundle=... com a versão de bundletool aprovada pelo projeto e guardar SHA-256 do AAB. Reinspecionar o manifesto e dependências do artefato assinado. Não considerar apenas “BUILD SUCCESSFUL” como validação de assinatura.

## 6. Testes em dispositivos reais

A validação final exige dispositivos Android físicos; CI/emulador não substitui os testes de permissões, MediaStore, notificações e restrições do fabricante.

### Matriz mínima

| Cenário | Cobertura |
|---|---|
| Android 8/8.1 (API 26/27) | Instalação, biblioteca, câmera, compartilhamento |
| Android 12/12L (API 31/32) | Permissões de mídia legadas e notificações |
| Android 13 (API 33) | READ_MEDIA_IMAGES, POST_NOTIFICATIONS, Photo Picker |
| Android 14/15+ (API 34/35) | Acesso parcial vs completo, READ_MEDIA_VISUAL_USER_SELECTED, FGS specialUse |
| Android 16 / API 36 | Target atual, FGS, permissões, compatibilidade de bundle e comportamento do sistema |

### Checklist por dispositivo

- [ ] Instalar o AAB assinado por faixa de teste interno ou APKs gerados com bundletool.
- [ ] Iniciar o app sem conceder permissões e confirmar que as funções não dependentes continuam utilizáveis.
- [ ] Importar uma imagem manualmente pelo Photo Picker.
- [ ] Testar câmera e OCR offline; confirmar que o texto é reconhecido localmente.
- [ ] Conceder acesso parcial no Android 14+ e confirmar que o serviço contínuo não inicia.
- [ ] Conceder acesso completo e ativar o monitoramento por ação explícita.
- [ ] Confirmar notificação persistente, ação Desativar e encerramento do serviço.
- [ ] Criar screenshots em pelo menos dois apps; confirmar detecção, deduplicação e prévia.
- [ ] Confirmar que nenhuma screenshot vira referência sem escolha de categoria e confirmação.
- [ ] Confirmar que a imagem original permanece na galeria.
- [ ] Revogar permissões durante o uso e confirmar encerramento/estado coerente, sem crash.
- [ ] Testar reinício do processo e reinicialização do dispositivo; o serviço não deve iniciar automaticamente no boot.
- [ ] Testar bateria/restrições do fabricante e registrar quando o sistema interrompe o serviço.
- [ ] Testar compartilhamento PDF, backup ZIP e restauração aditiva.
- [ ] Testar rede: importar link, YouTube oEmbed, feed e WebView; registrar destinos de rede e confirmar ausência de upload de imagens/OCR para servidor próprio.
- [ ] Testar atualização sobre instalação anterior na faixa interna.
- [ ] Verificar layout, fonte ampliada, modo claro/escuro e navegação do sistema.

Registrar modelo, fabricante, versão Android, build do app, passos, resultado, logs e evidências. Não marcar um teste como aprovado sem executá-lo.

## 7. Critério de conclusão

O release só estará pronto quando:
- política publicada refletir a seção ML Kit;
- Data Safety estiver preenchido com base no AAB assinado e no comportamento observado;
- declarações READ_MEDIA_IMAGES e FGS forem submetidas sem alegações enganosas;
- vídeo demonstrar o fluxo real;
- AAB assinado e validado;
- testes físicos aprovados;
- riscos de elegibilidade aceitos conscientemente pelo desenvolvedor.
