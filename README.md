# CaptureFlow

CaptureFlow é um app Android-first para guardar links, textos e imagens como referências organizadas e apoiar a execução com sub-checklists por referência.

## Stack canônica

- Kotlin + Android SDK + Jetpack Compose / Material 3
- MVVM com Android ViewModel, Coroutines e Flow/StateFlow
- Room / SQLite para persistência local
- Android Sharesheet / `ACTION_SEND` para texto, URLs e imagens
- Android Photo Picker para importar imagens
- Google ML Kit Text Recognition para OCR on-device
- Gradle Kotlin DSL

A implementação antiga Expo/React Native está em [`legacy-expo/`](legacy-expo/) apenas como histórico. Não desenvolver recursos novos nela.

## Funcionalidades atuais

- Capturar texto e links compartilhados de outros apps.
- Importar imagens via Photo Picker ou compartilhar imagens com CaptureFlow.
- Persistir referências localmente, com cópia privada da imagem quando possível.
- Buscar referências e filtrar por categoria.
- Alterar categoria, concluir e excluir referências.
- Extrair título de imagens automaticamente usando OCR local.
- Abrir imagem em tela cheia, navegar entre referências com swipe horizontal e abrir a sub-checklist da referência.
- Criar, concluir e remover subtarefas persistidas por referência.
- Monitoramento opcional de screenshots com notificação persistente e confirmação para importar a imagem como referência; detecção heurística, não garantida.

## Abrir e compilar

Requisitos: Android Studio, Android SDK 36, JDK 17+ (JDK 21 é adequado) e acesso à rede para baixar dependências Gradle/Maven.

1. Clone o repositório.
2. Abra `android-native/` no Android Studio.
3. Aguarde a sincronização do Gradle e execute a configuração `app` em emulador/dispositivo.

No terminal PowerShell:
```powershell
cd android-native
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

APK debug esperado: `android-native/app/build/outputs/apk/debug/app-debug.apk`.

## Documentação

- [Visão geral do projeto, arquitetura, UI e fluxos](docs/project-overview.md)
- [Diretrizes de engenharia](AGENTS.md)
- [Arquitetura](docs/architecture.md)
- [Plano de migração](docs/migration-plan.md)
- [Roadmap](docs/roadmap.md)
- [Captures — documentação da feature](docs/features/captures/README.md)
- [Checklist e sub-checklist](docs/features/checklists.md)
- [Diagrama do sistema](docs/SD_captureflow.drawio)

## Privacidade

Referências e imagens são armazenadas localmente; não há conta, analytics nem sincronização cloud implementada. OCR é executado localmente. O serviço de metadados faz requisições HTTP(S) para URLs compartilhadas pelo usuário. O monitoramento de screenshots é opt-in e depende de permissões, MediaStore e restrições do dispositivo.

## Estado de validação

A documentação descreve o código observado, mas não substitui a validação em runtime. O PR #25 contém uma correção de imports Kotlin e deve ter CI/build aprovado antes de declarar a versão atual compilável. Consulte [GitHub Actions](https://github.com/FeHunter/captureflow/actions) para o resultado do commit mais recente.
