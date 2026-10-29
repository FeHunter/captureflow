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


## Atualização de navegação, imagem e tema
- A tela de Referências é a tela principal única; a checklist independente aparece abaixo da biblioteca, eliminando o bottom navigation.
- Ao selecionar uma imagem, abrir um diálogo de categoria/tag (Ler depois, Ideias, Tarefas ou Referência) antes de persistir e iniciar OCR.
- O seletor de tema fica no cabeçalho e oferece Seguir dispositivo, Claro e Escuro.
- Paleta inspirada na arte Hunter: azul-marinho profundo, azul oceano, azul-ciano e branco; no tema escuro, superfícies azul-marinho em vez de preto puro.


## Simplificação da tela principal
- A tela principal mantém a seleção de imagem, a busca, os filtros e a lista de referências.
- Não exibir a seção independente Checklist nem o formulário manual de texto/link; compartilhar conteúdo de outros apps continua sendo uma forma de captura.
- Exibir um ícone X no campo de busca somente quando houver texto, permitindo limpar a consulta com um toque.
- A versão exibida no cabeçalho usa `BuildConfig.VERSION_NAME`, que recebe a versão base e o hash curto do commit atual no build.
