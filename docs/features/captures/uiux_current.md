# UI/UX atual — Captures

## Navegação
- Barra inferior com as abas **Referências** e **Checklist**.
- Cabeçalho mostra `CaptureFlow v{versionName}`, proveniente de BuildConfig.
- A área de referências contém seletor de imagem, entrada manual, busca, filtros e lista.

## Cartões de referência
- Conteúdo persistente visível limitado a: título, capa da imagem, checkbox de conclusão e botão de exclusão.
- A capa é exibida quando `imageUri` existe e a imagem pode ser carregada; tocar nela abre uma visualização fullscreen com botão de fechar.
- A análise OCR começa automaticamente quando uma imagem é selecionada ou compartilhada para Referências; não existe botão manual de análise no cartão.
- Enquanto a análise está em andamento, mostrar spinner e o texto curto `Analisando imagem…`.
- Se a análise falhar ou não reconhecer texto, mostrar uma mensagem de erro discreta; não exibir descrição, texto OCR bruto ou categoria no cartão.
- Cada cartão possui uma sub-checklist persistente com campo para adicionar itens, checkbox por item e ação para excluir item.

## Tarefas
- Título, checkbox de conclusão e exclusão.
- Capa de screenshot quando houver imagem.
- Descrição opcional e ação de análise.

## Regras
- Estados de erro não devem apagar conteúdo original.
- Imagem ausente/inacessível deve degradar para cartão sem capa, sem travar a lista.
- Layout deve permitir rolagem e evitar overflow em telas estreitas.
- Status e ações não podem depender exclusivamente de cor.
