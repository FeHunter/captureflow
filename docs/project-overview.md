# Capture Flow — visão geral

**Atualizado:** 2026-11-04, evolução proposta após PR #61. Caderno pessoal Android para links, textos e imagens como referências locais, organizadas por categoria, com subtarefas, anotações e backup. App ativo em android-native/; legacy-expo/ é histórico.

## Documentação
- [Estado atual](current-state.md) · [Arquitetura](architecture.md) · [Design System](design-system.md) · [Índice das features](features/README.md) · [AGENTS](../AGENTS.md)

## Stack
Kotlin, Android SDK, Jetpack Compose/Material 3, MVVM, Coroutines/Flow, Room v11, ML Kit OCR e SharedPreferences. Feature-first em um módulo Gradle.

## Features ativas
Captures, Image Import, Image Compression, Categories, Search, Dashboard, Fullscreen, YouTube Player, Notes, Newsletters, Settings/Privacidade/Tutoriais, Backup e Share.

## Produto
Biblioteca única sem bottom navigation; quatro categorias padrão; conclusão independente da categoria; captura por câmera/Photo Picker/Sharesheet/screenshot; OCR local; leitor web e player YouTube; feeds RSS/Atom; backup ZIP v2 aditivo com Newsletters; compressão adaptativa WebP/JPEG; compartilhamento Android, PDF e RTF.

## Privacidade e limites
Dados do caderno são locais; sem conta/sincronização própria. Links, feeds e páginas podem acessar serviços externos. Backup v2 inclui Newsletters; preferências permanecem fora do backup. Documentação não substitui CI/testes em dispositivo.
