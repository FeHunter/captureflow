# Capture Flow — visão geral do projeto

**Atualizado:** 2026-10-29  
**Escopo:** estado observado no código; não é promessa de release.

Capture Flow é um app Android para guardar links, textos e imagens compartilhados como referências locais, organizá-los por categoria e associar sub-checklists. A fonte de verdade é o app Kotlin/Jetpack Compose em `android-native/`; `legacy-expo/` é histórico.

## Documentação canônica
- **[Estado atual completo do app](current-state.md)** — arquitetura, telas, layout, gestos, compartilhamento, OCR, imagens, screenshots, banco e limites.
- [Arquitetura](architecture.md)
- [Feature Captures](features/captures/README.md)
- [Contrato funcional](features/captures/feature.md)
- [Contrato do banco](features/captures/database_contract_current.md)
- [UI/UX atual](features/captures/uiux_current.md)
- [Wireframes](features/captures/wireframes.md)
- [Diretrizes de engenharia](../AGENTS.md)

## Stack e arquitetura
- Kotlin, Android SDK, Jetpack Compose/Material 3, MVVM, Coroutines/Flow.
- Room/SQLite local (`captureflow.db`), versão observada 5.
- ML Kit Text Recognition para OCR no dispositivo.
- `LinkMetadataService` faz requisição HTTP(S) e tenta extrair H1, imagem do conteúdo e metadados fallback.
- Imagens são copiadas para armazenamento privado quando possível.
- Não há sincronização cloud implementada.

## Comportamento atual resumido
- Uma tela principal de referências, nome do produto **Capture Flow**, sem bottom navigation ou tela independente de tarefas. A biblioteca agrupa referências por categoria em cartões de largura total, com até três thumbnails e ação “Ver todas”; a categoria expandida usa grade de três colunas.
- Pesquisa por título/conteúdo, filtros `Ler depois`, `Verificar`, `Salvar`, `Importante` e ocultar/mostrar concluídas.
- Compartilhar URL abre um diálogo de prévia e exige categoria antes de persistir; cancelamento não salva.
- Texto sem URL é salvo diretamente. Imagens passam por cópia privada e OCR automático.
- Visualizador fullscreen usa swipe horizontal, título editável e imagem `ContentScale.Fit`; o Back nativo fecha a camada superior.
- Monitoramento de screenshots é opcional, baseado em metadados do MediaStore e best-effort.

## Limitações e pendências conhecidas
- Extração de metadados não executa JavaScript; sites com conteúdo montado no cliente, autenticação ou bloqueio podem não fornecer título/imagem. O link continua salvável com fallback.
- O serviço de notificação ainda contém linguagem histórica de tarefas, embora a experiência atual seja de referências.
- O cartão atual mostra o título também no campo secundário; não presumir descrição separada.
- Validar build/CI do commit exato e testar intents, metadados, OCR, permissões e layout em dispositivo real.
