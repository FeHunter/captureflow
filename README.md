# CaptureFlow

Aplicativo Android para organizar links, textos e imagens como referências em caderno pessoal, com categorias, subtarefas, anotações e backup local.

## Stack
Kotlin, Android SDK, Jetpack Compose/Material 3, MVVM, Coroutines/Flow, Room v11, Sharesheet, Photo Picker, CameraX, ML Kit OCR. App ativo em android-native/; legacy-expo/ é histórico.

## Funcionalidades
- Captura por câmera, Photo Picker e compartilhamento de texto, links e imagens.
- Prévia de metadados, confirmação de categoria e OCR local.
- Categorias, busca, ordenação e dashboard de progresso.
- Fullscreen, sub-checklist, Notes expansível, edição de capa, leitor web e player YouTube.
- Compressão adaptativa WebP/JPEG, com preservação de transparência.
- Newsletters RSS/Atom com atualização manual.
- Monitoramento opt-in de screenshots, best-effort.
- Backup ZIP v2 com referências, imagens e dados de Newsletters; importação v1/v2 aditiva.
- Compartilhamento de pensamentos via Android Sharesheet, PDF e RTF.
- Tema claro/escuro, Privacidade e Tutoriais.

## Compilar
Android Studio, Android SDK 36, JDK 17+.
```powershell
cd android-native
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
APK: android-native/app/build/outputs/apk/debug/app-debug.apk.

## Documentação
[Estado atual](docs/current-state.md) · [Visão geral](docs/project-overview.md) · [Arquitetura](docs/architecture.md) · [Features](docs/features/README.md) · [Design System](docs/design-system.md) · [AGENTS](AGENTS.md) · [Diagrama](docs/SD_captureflow.drawio)

## Privacidade
Dados do caderno são locais; sem conta/sincronização própria. Links, feeds e leitura web podem acessar serviços externos. Backup v2 inclui fontes e artigos de Newsletters; preferências continuam fora do backup.
