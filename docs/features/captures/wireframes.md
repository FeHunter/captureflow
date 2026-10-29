# Wireframes — Capture Flow atual

Os esquemas abaixo registram a estrutura ativa observada no código. Não representam pixels exatos; validar dimensões e fonte em dispositivo.

## 1. Tela principal

```text
┌──────────────────────────────────────────┐
│ Capture Flow                         [⋮] │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ │
│ │ Analisar imagem do dispositivo       │ │
│ │ [Selecionar imagem]                  │ │
│ │ [Configurar/Ativar/Desativar avisos] │ │
│ └──────────────────────────────────────┘ │
│ ┌──────────────────────────────────────┐ │
│ │ 🔍 Pesquisar…                     [X]│ │
│ └──────────────────────────────────────┘ │
│ [Todas] [Ler depois] [Verificar] →       │
│ ┌──────────────────────────────────────┐ │
│ │ Ler depois                     12 itens│
│ │ ┌────────┐ ┌────────┐ ┌────────┐      │
│ │ │ thumb  │ │ thumb  │ │ thumb  │      │
│ │ │ título │ │ título │ │ título │      │
│ │ └────────┘ └────────┘ └────────┘      │
│ │             [⌄ Ver todas · 12]        │
│ └──────────────────────────────────────┘ │
│ ┌──────────────────────────────────────┐ │
│ │ Verificar                      4 itens│
│ │ [thumb] [thumb] [thumb]               │
│ │             [⌄ Ver todas · 4]         │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

- Rolagem vertical para conteúdo; chips de categoria rolam horizontalmente.
- Grupos de categoria ocupam a largura disponível. Cada um mostra até três thumbnails e a ação “Ver todas”.
- Menu superior permite ocultar/mostrar concluídas e alterar tema (sistema/claro/escuro).

## 2. Categoria expandida

```text
┌──────────────────────────────────────────┐
│ [←] Ler depois                           │
│     12 itens                             │
│ ┌────────┐ ┌────────┐ ┌────────┐          │
│ │ thumb  │ │ thumb  │ │ thumb  │          │
│ └────────┘ └────────┘ └────────┘          │
│ ┌────────┐ ┌────────┐ ┌────────┐          │
│ │ thumb  │ │ thumb  │ │ thumb  │          │
│ └────────┘ └────────┘ └────────┘          │
└──────────────────────────────────────────┘
```

- Modal fullscreen, botão voltar/fechar, categoria e contagem.
- Grade de três colunas; thumbnails quadrados com imagem `ContentScale.Crop`.
- Back nativo fecha a categoria se nenhum viewer estiver por cima.

## 3. Visualizador fullscreen da referência

```text
┌──────────────────────────────────────────┐
│ 29/10/2026 · 10:35 [Ler depois] [🔗][□][🗑][X]│
├──────────────────────────────────────────┤
│ ████████████████████████████████████████ │
│ █ Título da referência                  █ │
│ █ gradiente escuro                      █ │
│ █                                        █ │
│ █             IMAGEM INTEGRAL            █
│ █            ContentScale.Fit            █
│ █                                        █
│ █       ‹ Deslize para navegar ›         █
└──────────────────────────────────────────┘
```

- A imagem ocupa a área disponível sem crop. O título e a dica são overlays; não alteram a medição da imagem.
- Swipe-left: próxima referência na mesma categoria filtrada. Swipe-right: anterior. Sem loop.
- A barra mostra data, categoria, abrir link original (ícone 🔗 somente se `sourceUrl` for HTTP(S) válido), conclusão, excluir e fechar.
- Back fecha apenas o viewer; a categoria expandida permanece por baixo.

## 4. Compartilhamento de link — confirmação de categoria

```text
┌──────────────────────────────────────────┐
│ Salvar link como referência              │
│ Título encontrado (H1/fallback)           │
│ https://site.example/article              │
│ Descrição, se encontrada                  │
│ Buscando título e imagem do site…         │
│ Em qual categoria deseja salvar?          │
│ ○ Ler depois                              │
│ ○ Verificar                               │
│ ○ Salvar                                  │
│ ○ Importante                              │
│ [Cancelar]             [Salvar referência]│
└──────────────────────────────────────────┘
```

- Sem categoria pré-selecionada; salvar desabilitado enquanto a requisição está em andamento.
- Cancelar não persiste. Se a extração falhar, o link continua salvável com título fallback.
- Título prioriza H1; imagem prioriza article/main e depois metadados Open Graph/Twitter.
- Imagem de capa é baixada após confirmar a categoria. O parser não executa JavaScript.

### Captura sem URL de origem
Quando o screenshot é importado sem URL explícita, o CaptureFlow mantém a imagem e o título extraído pelo OCR. Endereços reconhecidos na barra do navegador não viram link de origem automaticamente; o usuário pode compartilhar o conteúdo pelo fluxo nativo do Android quando o app de origem oferecer essa opção.


## 5. Seleção de imagem e sub-checklist
- Seleção de imagem: diálogo de categoria antes de importar; imagem é copiada para armazenamento privado quando possível e OCR é iniciado.
- Sub-checklist: `ModalBottomSheet` com progresso, lista de itens, checkbox, excluir e campo de nova subtarefa.
- O monitor de screenshots é opt-in e best-effort; strings históricas de notificação ainda podem mencionar tarefas.

## 6. Layout e acessibilidade
- Manter alvo de toque de ícones em 48 dp, rótulos de acessibilidade, contraste e tema claro/escuro.
- Testar telas estreitas, fontes ampliadas, imagens ausentes, títulos longos, gestos horizontais e Back do Android.
