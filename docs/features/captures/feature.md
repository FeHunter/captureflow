# Contrato funcional — Captures

**Versão documentada:** 0.3.3 (confirmar configuração Gradle antes de release).  
**Estado:** código em evolução; verificação de build e teste em dispositivo são gates separados.

## Objetivo
Guardar links, textos e imagens como referências locais pesquisáveis. Cada referência pode ter uma sub-checklist persistente. Não existe uma tela principal independente de Tarefas.

## Tela principal
1. Top bar com versão e menu de tema (seguir sistema, claro, escuro).
2. Cartão para selecionar imagem e mostrar/configurar monitoramento de screenshots.
3. Campo de pesquisa, com X para limpar quando preenchido.
4. Chips: Todas, Ler depois, Ideias, Tarefa, Importante e Mais tarde.
5. Biblioteca rolável em duas colunas de cartões.

## Fluxos
### Texto e URL compartilhados
- Entrada por Android Sharesheet/Intent.
- Normalização e classificação via `CaptureClassifier`.
- Texto normalizado é deduplicado quando já existe.
- URL HTTP(S) pode ser enriquecida com título/descrição de metadados HTML; a URL original continua disponível se o enriquecimento falhar.

### Imagem
- A imagem pode vir do Photo Picker, de um compartilhamento ou da notificação de screenshot.
- O app tenta copiar a imagem para armazenamento privado e salva a referência em Room.
- O OCR ML Kit é iniciado automaticamente; o título heurístico é persistido, e a imagem continua como capa.
- Para referências de imagem, o fluxo atual grava título e mantém `content` vazio; o texto OCR completo não é mostrado no cartão nem persistido como conteúdo dessa referência.
- Erro ou texto não reconhecido deve preservar a referência e a imagem.

### Screenshot via notificação
- O usuário ativa/desativa o monitoramento explicitamente.
- O serviço foreground observa MediaStore e identifica candidatos por nome/pasta, sem analisar pixels da tela de outros apps.
- Tocar na notificação abre a Activity com uma URI pendente e exibe confirmação.
- Confirmar chama `addImageCapture(uri, category = "Referência")`; a imagem vira `CaptureEntity` na biblioteca Referências.
- Cancelar/fechar não cria uma referência.
- A detecção é best-effort, limitada por permissões, MediaStore, nomes de arquivos e políticas de bateria/fabricante.

**Inconsistência conhecida:** algumas strings de notificação/modal e a ação interna ainda dizem “tarefa” (`ACTION_CREATE_TASK`), embora o fluxo confirmado agora crie referência. Corrigir a nomenclatura sem reintroduzir aba de tarefas.

## Cartões e detalhes
- Cartão nominal de 320 dp, capa de 104 dp e grade de duas colunas.
- Título, texto secundário, categoria/menu, data/hora, checkbox, progresso de sub-checklist e exclusão.
- A capa abre detalhes fullscreen; a imagem usa `ContentScale.Fit`.
- Swipe para esquerda vai à próxima referência; para direita, à anterior. A navegação segue a lista fornecida/filtrada e não faz loop.
- O rótulo `‹ Deslize para navegar ›` é sobreposto na parte inferior da imagem e não reserva espaço.
- Atalho no cabeçalho fullscreen abre a sub-checklist da referência atual.

## Sub-checklist
- Cada item é salvo em `capture_subtasks` e relacionado por `captureId`.
- Usuário pode adicionar, concluir e excluir itens.
- Ao excluir referência, seus itens associados são removidos antes da referência.

## Categorias e consistência
Os filtros atuais são Todas, Ler depois, Ideias, Tarefa, Importante e Mais tarde. `Todas` é apenas filtro. O fluxo de notificação usa categoria `Referência`, ainda não incluída de forma consistente na lista de categorias da UI. Não normalizar isso sem uma decisão de produto.

## Privacidade e limites
- Dados locais; sem sincronização cloud implementada.
- OCR local com ML Kit.
- Requisição de metadados de link faz acesso de rede à URL compartilhada.
- Cópia de imagem privada é tentada; URI original é fallback.
- OCR e detecção de screenshot são heurísticos; não garantem título/detecção em todos os layouts/dispositivos.
- Foreground service `specialUse` exige revisão de política do Google Play antes de distribuição.

## Validação necessária
- Build/testes do commit atual.
- Share intents de texto, URL e imagem; Photo Picker; URI permissions.
- OCR em imagens variadas, sem texto e com erro de leitura.
- Notificação, confirmação/cancelamento e persistência.
- Swipe nas extremidades, filtros e sub-checklist.
- Permissões e restrições de bateria em dispositivo real.


### Navegação e correção manual do título

O visualizador fullscreen usa `HorizontalPager` para navegar suavemente pela lista filtrada. A ação **Editar título** está restrita ao detalhe fullscreen e persiste somente o título da referência. O acesso à sub-checklist continua no cartão de referência.


### Navegação horizontal e título editável

O visualizador fullscreen usa `HorizontalPager`. A ação **Editar título** fica restrita ao detalhe fullscreen e atualiza somente o título persistido. O acesso à sub-checklist permanece no cartão de referência.


## OCR centralizado, fullscreen e limpeza de screenshots
- A seleção do título OCR dá peso adicional à região central da imagem, refletindo o comportamento de usuários que rolam o conteúdo relevante para o centro antes de capturar.
- O fullscreen usa proporção fixa aproximada de 20% para cabeçalho e 80% para imagem; títulos longos recebem overlay com gradiente escuro que se dissolve para transparente.
- Screenshots abertas pela notificação e confirmadas para importação podem ser removidas da galeria depois que a cópia privada e a referência forem salvas. A seleção manual de imagens não apaga o original.
- A exclusão de mídia compartilhada é best-effort: versões recentes do Android podem exigir consentimento do usuário para apagar arquivos criados por outro app; se o sistema bloquear, a captura original permanece.
