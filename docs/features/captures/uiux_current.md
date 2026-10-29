# UI/UX atual — Captures

## Navegação
- Barra inferior com as abas **Referências** e **Checklist**.
- Cabeçalho mostra `CaptureFlow v{versionName}`, proveniente de BuildConfig.
- A área de referências contém seletor de imagem, entrada manual, busca, filtros e lista.

## Cartões de referência
- Checkbox e ação de exclusão.
- Título e categoria.
- Capa quando `imageUri` existe e a imagem pode ser carregada.
- Texto/descrição do conteúdo.
- Ação de OCR para imagens e feedback de processamento/erro.

## Tarefas
- Título, checkbox de conclusão e exclusão.
- Capa de screenshot quando houver imagem.
- Descrição opcional e ação de análise.

## Regras
- Estados de erro não devem apagar conteúdo original.
- Imagem ausente/inacessível deve degradar para cartão sem capa, sem travar a lista.
- Layout deve permitir rolagem e evitar overflow em telas estreitas.
- Status e ações não podem depender exclusivamente de cor.
