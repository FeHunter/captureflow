# Fullscreen

HorizontalPager navega na categoria/estado selecionados; swipe esquerda avança, direita retorna, sem loop. Back fecha camada superior. Imagem Fit em fundo escuro; título logo abaixo do header/sobre mídia com gradiente.

Header: Compartilhar pensamento e Abrir link externo se URL válida. Dock vertical: concluir, sub-checklist, Visualizar conteúdo (globo), editar título/categoria e excluir conforme estado. O modal Editar título contém os campos Título principal e Categoria e uma ação dedicada Alterar imagem da capa; a ação abre o Photo Picker e mantém o pipeline de compressão/substituição. Concluídas mostram só reabrir; Notes permanece independente no canto inferior esquerdo; quando há blocos, o cabeçalho do painel expande/recolhe a área de leitura (estado somente de UI). Estado persistente vem de Captures por callbacks.

CaptureWebReader: JS/DOM Storage off, file/content access e mixed content bloqueados, Safe Browsing e navegação HTTP/HTTPS com host válido. Back percorre histórico. YouTube usa player dedicado.