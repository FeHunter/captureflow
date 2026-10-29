# Acesso a dados e ações — Captures

## Estado observável
`CaptureViewModel` expõe Flow/StateFlow para referências, tarefas mantidas no modelo e sub-checklists. A UI coleta estado e encaminha eventos; não consulta DAO diretamente.

## Referências
- `addCapture(raw)`: normaliza e classifica conteúdo; evita duplicatas textuais e tenta enriquecer URL.
- `addImageCapture(uri, category)`: tenta copiar imagem para armazenamento privado, cria `CaptureEntity` e inicia OCR.
- `setCaptureCompleted` e `setCaptureCategory`: atualizam campos persistidos.
- `deleteCapture`: tenta remover arquivo privado, apaga sub-itens associados e remove a referência.
- `extractText`: executa OCR em background, atualiza estado de processamento/erro e persiste o título sugerido.

## Imagens
`SharedImageStorageService` tenta copiar para `filesDir/captures`. Se falhar, mantém a URI original como fallback. Exclusão deve apagar apenas arquivos privados que pertencem ao app; nunca apagar arquivos externos fornecidos pelo sistema/provedor.

## Links
`LinkMetadataService` procura metadados HTML comuns (`og:title`, `og:description`, Twitter Card e tags padrão). Há limites de timeout/tamanho e redirecionamentos automáticos desativados. Se a busca falhar, manter o conteúdo/URL original.

## Sub-checklists
- Inserir item com UUID, `captureId`, título, estado inicial incompleto e timestamp.
- Marcar conclusão e remover por ID.
- Excluir os itens associados antes de excluir uma referência.

## Tarefas legadas no modelo
O banco/ViewModel ainda possui `ChecklistTaskEntity`, `checklistDao`, operações de criar/concluir/categorizar/excluir tarefa e análise de imagem de tarefa. A tela atual não expõe uma aba/lista de tarefas independente. Não conectar novamente esses fluxos à UI sem decisão explícita de produto.

## Screenshot da notificação
A confirmação atual chama `addImageCapture(uri, category = "Referência")`. Embora o nome da ação interna ainda possa conter `CREATE_TASK`, o resultado esperado é uma referência, não `ChecklistTaskEntity`.
