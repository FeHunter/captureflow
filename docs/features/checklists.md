# Checklists

## Estado atual da experiência

A tela principal do CaptureFlow é única: **Referências**. Não existe aba ou tela independente de Tarefas na interface atual.

A funcionalidade de checklist exposta na UI é uma **sub-checklist vinculada a cada referência**. Ela é aberta pelo botão de progresso no cartão ou pelo atalho de checklist no visualizador fullscreen.

## Comportamentos
- Adicionar uma subtarefa com título não vazio.
- Marcar/desmarcar conclusão.
- Excluir subtarefa.
- Exibir progresso concluído/total.
- Persistir cada item localmente em Room, tabela `capture_subtasks`, ligado à referência por `captureId`.
- Ao excluir uma referência, remover os itens associados.

## Compatibilidade
O modelo de dados ainda contém `ChecklistTaskEntity` e operações de tarefa independentes no ViewModel. Isso não significa que exista uma tela de tarefas acessível na UI atual. Não reintroduzir essa navegação sem decisão explícita de produto.

## Implementação
- UI: `ui/CaptureFlowApp.kt`
- Estado e operações: `ui/CaptureViewModel.kt`
- Entidades/DAO/migração: `data/CaptureDatabase.kt`
- Contrato funcional: [captures/feature.md](captures/feature.md)
