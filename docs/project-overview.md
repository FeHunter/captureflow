# CaptureFlow — visão geral do projeto

**Atualizado:** 2026-10-29  
**Escopo:** estado observado no repositório, não promessa de release.

## 1. Produto

CaptureFlow é um app Android-first para guardar links, textos e imagens como referências, encontrar conteúdo por busca/categoria e transformar referências em itens acionáveis por meio de sub-checklists. A prioridade atual é captura rápida e organização local, com OCR de imagens no próprio aparelho.

## 2. Estado atual e fonte de verdade

- App canônico: `android-native/` (Kotlin, Android SDK, Jetpack Compose).
- Implementação anterior Expo/React Native: `legacy-expo/`, mantida como histórico; não adicionar recursos novos nela.
- Persistência: Room/SQLite local, banco `captureflow.db`.
- Arquivos de implementação: `android-native/app/src/main/java/com/fehunter/captureflow/`.
- Contrato e detalhes de feature: [docs/features/captures/README.md](features/captures/README.md).
- Diretrizes para agentes/desenvolvimento: [AGENTS.md](../AGENTS.md).

A documentação histórica contém descrições de interfaces e fluxos anteriores. Em caso de divergência, o código da branch principal e este resumo de estado devem ser verificados; não assumir que wireframes antigos descrevem a UI executada.

## 3. Arquitetura

```text
Android Sharesheet / Photo Picker / Notification
                    |
                    v
              MainActivity
       intents, URI e ciclo de vida
                    |
                    v
       CaptureFlowApp (Jetpack Compose)
       tela única de Referências
                    |
                    v
            CaptureViewModel
       estado observável + casos de uso
         |          |          |
         v          v          v
       Room       OcrService  LinkMetadataService
    SQLite/DAO    ML Kit      HTML HTTP(S)
         |
         v
 SharedImageStorageService
 arquivos privados do app
```

O monitoramento de screenshots é um serviço Android opcional e ativado pelo usuário (`ScreenshotNotificationService`), separado da tela Compose. Existe também `ScreenshotMonitor`, um observador para uso em primeiro plano. Ambos usam metadados do MediaStore e heurísticas de nome/pasta; não leem pixels da tela de outros apps e não usam Accessibility Service.

### Responsabilidades por camada

- **MainActivity:** ponto de entrada Android; trata intents compartilhadas e intents de retorno de notificação e entrega o conteúdo à UI/ViewModel.
- **UI Compose (`CaptureFlowApp.kt`):** apresenta biblioteca, pesquisa, chips de categoria, seleção de imagem, diálogos, cartões, detalhe fullscreen e sub-checklist. Não deve acessar DAOs diretamente.
- **ViewModel (`CaptureViewModel.kt`):** coordena operações assíncronas, expõe Flow/StateFlow, aplica regras de persistência e chama serviços.
- **Data (`CaptureDatabase.kt`):** entidades, Room database, DAOs e classificação de conteúdo.
- **Services:** OCR local, metadados HTTP, cópia/remoção de imagem e detecção/notificação de screenshot.

## 4. Interface implementada

A tela principal é única: **Referências**. Não há navegação inferior nem tela separada de Tarefas.

Ordem geral da tela:
1. Top bar com nome/versão e seletor de tema.
2. Cartão de entrada para selecionar uma imagem e instruções/controles de monitoramento de screenshot.
3. Campo de pesquisa com ação de limpar quando preenchido.
4. Chips horizontais de categoria.
5. Biblioteca em grade de duas colunas com rolagem vertical.

### Cartão de referência

- Altura nominal atual: 320 dp.
- Slot de imagem/capa: 104 dp; recorte `ContentScale.Crop` no cartão.
- Título principal e texto secundário truncados em até duas linhas cada.
- Chip de categoria com menu para alterar a categoria.
- Data/hora local a partir de `createdAt`.
- Checkbox de conclusão, botão de sub-checklist com progresso e ação de exclusão.
- Imagens inacessíveis ou ausentes usam placeholder; OCR pode exibir estado de processamento/erro.

Categorias/filtros visíveis: `Todas` (filtro), `Ler depois`, `Ideias`, `Tarefa`, `Importante`, `Mais tarde`. A categoria `Referência` também é usada pelo fluxo de screenshot da notificação e pode não estar incluída na lista de filtros/categorias do cartão; revisar essa consistência ao evoluir a UI.

### Detalhe fullscreen

- Cabeçalho com título, data, categoria, atalho para sub-checklist e fechar.
- Imagem ajustada por `ContentScale.Fit`, preservando proporção.
- Navegação horizontal implementada com `HorizontalPager` do Compose Foundation: deslizar para a esquerda avança para a próxima referência; deslizar para a direita retorna à anterior, com animação/gesto nativos de pager.
- O pager usa a lista fornecida ao cartão (incluindo busca e filtro ativos) e não faz loop nas extremidades.
- A página selecionada sincroniza o título, data e categoria do cabeçalho; cada página carrega a imagem correspondente com `ContentScale.Fit`.
- A dica visual `‹ Deslize para navegar ›` permanece sobreposta à imagem e não reserva espaço de layout.
- O cabeçalho do fullscreen tem ação **Editar título**. O diálogo permite corrigir o título sugerido pelo OCR; salvar persiste apenas `captures.title`, sem sobrescrever conteúdo ou imagem. A edição está disponível somente no fullscreen.

### Sub-checklist

Cada referência pode ter itens persistentes adicionados, concluídos e removidos. Abre em `ModalBottomSheet`, mostra progresso e campo para nova subtarefa. Os itens vivem em tabela Room separada e pertencem à referência por `captureId`.

## 5. Como os recursos funcionam

### Compartilhar texto ou link
1. O usuário escolhe CaptureFlow no Android Sharesheet.
2. `MainActivity` recebe o Intent; `CaptureClassifier` normaliza/classifica o conteúdo.
3. `CaptureViewModel` evita duplicação de conteúdo textual quando aplicável e insere uma `CaptureEntity`.
4. Para URL HTTP(S), `LinkMetadataService` tenta obter título/descrição de metadados HTML públicos. Se falhar, o conteúdo original permanece.

A tela não tem formulário manual de texto/link; compartilhar a partir de outro app é o fluxo previsto.

### Importar imagem
1. O seletor de imagem do Android retorna uma URI.
2. `SharedImageStorageService` tenta copiar o arquivo para o diretório privado do app.
3. O ViewModel cria uma `CaptureEntity` com categoria e URI persistida (ou URI original como fallback).
4. O OCR local é iniciado automaticamente.
5. O título sugerido é salvo; a imagem original continua como capa. O texto OCR bruto não é exibido no cartão nem gravado no campo `content` da referência por esse fluxo.

### OCR
- `OcrService` usa ML Kit Text Recognition no dispositivo.
- Extrai linhas e caixas delimitadoras, ordena-as por posição e usa tamanho/posição/comprimento e heurísticas por família de UI (vídeo, social, notícias, compras, produtividade) para sugerir título.
- Filtra ruído conhecido de interface. É uma heurística, não entendimento semântico garantido.
- O estado de UI é exposto em `ocrStates`; erro não deve apagar a referência/imagem.

### Screenshot via notificação
- O usuário ativa explicitamente o monitoramento.
- `ScreenshotNotificationService` inicia um foreground service, registra observador no MediaStore e procura imagens recentes por nome/pasta reconhecível.
- Uma notificação abre a Activity com a URI pendente; o fluxo atual de confirmação chama `addImageCapture(uri, category = "Referência")`.
- A detecção é best-effort: depende de permissões, MediaStore, nomes/pastas e restrições do fabricante/bateria.
- **Atenção à consistência:** o serviço ainda usa a ação/linguagem histórica `ACTION_CREATE_TASK` / “pronta para tarefa”, enquanto a UI atual salva como referência. Também há texto no modal que menciona anexar a uma nova tarefa. Corrigir nomes e mensagens para refletir o comportamento final, sem reintroduzir uma aba Tarefas.

### Tarefas e compatibilidade de dados
O banco e o ViewModel ainda contêm `ChecklistTaskEntity`, DAO e operações de tarefa (incluindo análise OCR de screenshot anexada). Porém, a tela principal atual não renderiza uma lista independente de tarefas. A experiência de checklist exposta na UI é a sub-checklist por referência. Não confundir entidades/funcionalidade legada de dados com navegação ativa.

## 6. Dados locais

Tabelas Room documentadas:
- `captures`: referência, conteúdo, tipo, categoria, URL de origem opcional, URI da imagem, data e conclusão.
- `checklist_tasks`: entidade de tarefa mantida no modelo de dados.
- `capture_subtasks`: subtarefas associadas a uma referência.

Versão Room observada na documentação: 4. Migrações existentes: v1→v2 (`imageUri` em tarefas), v2→v3 (`description` em tarefas), v3→v4 (sub-checklist e índice por `captureId`). Confirmar `CaptureDatabase.kt` antes de alterar entidades/migrações.

## 7. Privacidade e limites

- Referências e imagens ficam no dispositivo; não há sincronização cloud implementada.
- OCR é local. A obtenção de metadados de links faz requisição de rede para a URL fornecida.
- Cópias privadas de imagens facilitam acesso após expirar a permissão temporária do provedor; se a cópia falhar, a URI original pode deixar de estar acessível.
- O monitor de screenshot é opcional, detecta por metadados e não é garantido em todos os fabricantes.
- O serviço usa tipo foreground `specialUse`; revisar as regras atuais do Google Play e a declaração necessária antes de publicar.
- Não usar Accessibility Service nem ler conteúdo visual de outros aplicativos para detecção.

## 8. Build, testes e validação

A partir de `android-native/`:
```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

CI: [Android CI](https://github.com/FeHunter/captureflow/actions).

Não declarar build ou testes aprovados sem verificar o resultado do commit correspondente. A falha Kotlin recente foi rastreada a imports ausentes para `KeyboardOptions` e `ImeAction`; a correção está proposta no PR #25 e precisa de CI verde antes de ser considerada validada. Testes de dispositivo ainda são necessários para intents, persistência de URI, OCR, permissões e monitoramento.

## 9. Pendências prioritárias

1. Confirmar PR #25 e CI/build em `main`.
2. Alinhar a linguagem da notificação e do diálogo ao comportamento “Adicionar às referências”.
3. Revisar categorias: `Referência` é usada ao salvar screenshots, mas não consta na lista atual de chips/menu de categoria.
4. Testar screenshot em dispositivos reais, permissões, notificação, restrições de bateria e reabertura via `onNewIntent`.
5. Testar importação por Sharesheet/Photo Picker e URIs de provedores diferentes.
6. Confirmar consistência do cartão (título secundário atualmente usa `capture.title` novamente) e acessibilidade/layout com fontes ampliadas.
7. Expandir testes de normalização de URL, deduplicação, OCR e regras de subtarefa.

## 10. Histórico recente relevante

- PR #22: introduziu fluxo de screenshot por notificação e navegação por swipe no fullscreen.
- PR #23: chegou a introduzir uma tela/aba independente de tarefas, mas a direção foi corrigida depois.
- PR #24: removida a navegação inferior/tela independente; screenshot de notificação volta a ser `CaptureEntity` na tela Referências; dica de swipe sobreposta. Merge commit `0dfe7157fff10dcbc2bb95d750dd99b54b869e79`.
- PR #25: correção de imports Compose (`KeyboardOptions` e `ImeAction`) integrada em 2026-10-29; validar o CI do commit atual.


## Atualização de UX — 2026-10-29

- O detalhe fullscreen usa `HorizontalPager` do Jetpack Compose em vez de detectar manualmente o fim de um gesto de arraste. A mudança de página é contínua, mantém a lista filtrada e não faz wrap-around.
- O título do cabeçalho acompanha a página ativa. A imagem de cada página mantém `ContentScale.Fit` e a dica de navegação continua como overlay.
- A ação do cabeçalho que abria a sub-checklist foi substituída por **Editar título**. A sub-checklist continua disponível pelo controle do cartão; não foi removida do produto.
- O diálogo de edição aparece somente no fullscreen e grava apenas o campo `captures.title` via ViewModel/DAO, preservando conteúdo, categoria e imagem.


## Atualização de UX — 2026-10-29

O detalhe fullscreen agora usa `HorizontalPager` em vez de detectar manualmente o fim de um arraste. O título principal pode ser corrigido no fullscreen; a atualização passa por `CaptureViewModel.updateCaptureTitle` e `CaptureDao.updateTitle`, sem sobrescrever os demais campos. O atalho de sub-checklist saiu do cabeçalho fullscreen, mas o controle no cartão permanece.
