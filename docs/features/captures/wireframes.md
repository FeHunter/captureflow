# Wireframes — tela única de Referências

Este documento descreve a estrutura funcional observada no código atual. Medidas são referências de implementação, não substituem teste em dispositivo.

## 1. Tela principal

```text
┌─────────────────────────────────────────┐
│ CaptureFlow v{versão}             [tema]│
│ Suas referências, organizadas           │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ Analisar imagem do dispositivo      │ │
│ │ Escolha foto ou screenshot          │ │
│ │ [Selecionar imagem]                 │ │
│ │ [Configurar/Desativar avisos]       │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ 🔍 Pesquisar…                    [X]│ │
│ └─────────────────────────────────────┘ │
│ [Todas] [Ler depois] [Ideias] [Tarefa] →│
│ ┌────────────────┐ ┌────────────────┐  │
│ │ Capa 104 dp    │ │ Capa 104 dp    │  │
│ ├────────────────┤ ├────────────────┤  │
│ │ Título         │ │ Título         │  │
│ │ Subtítulo      │ │ Subtítulo      │  │
│ │ [Categoria ▾]  │ │ [Categoria ▾]  │  │
│ │ Data · hora    │ │ Data · hora    │  │
│ │ □  ☰ 0/0   🗑  │ │ □  ☰ 2/3   🗑  │  │
│ └────────────────┘ └────────────────┘  │
└─────────────────────────────────────────┘
```

### Regras observadas
- Uma tela principal, sem bottom navigation e sem tela independente de Tarefas.
- A tela rola verticalmente; chips de categoria rolam horizontalmente.
- Busca e filtro de categoria funcionam juntos; o X aparece quando a busca contém texto.
- Categorias do filtro: Todas, Ler depois, Ideias, Tarefa, Importante, Mais tarde.
- O fluxo de screenshot usa a categoria `Referência`, que ainda não está presente na lista padrão de chips/menu.

## 2. Cartão

- Largura: divide a área disponível em duas colunas simétricas.
- Altura nominal: 320 dp.
- Capa: 104 dp, recortada com `ContentScale.Crop`; ícone de expandir no canto superior.
- Título principal e secundário limitados a duas linhas.
- Categoria selecionável por menu.
- Data/hora local no formato `dd/MM/yyyy · HH:mm`.
- Rodapé com checkbox de conclusão, botão de sub-checklist com progresso e excluir.
- Código atual mostra `capture.title` tanto no título principal quanto no texto secundário; não assumir que o segundo campo já mostra descrição distinta.
- A sub-checklist abre em bottom sheet e não expande o cartão na grade.

## 3. Modal de confirmação de screenshot

```text
┌───────────────────────────────────────┐
│ Adicionar screenshot às referências?  │
│ A captura foi aberta pela notificação.│
│ [prévia da imagem]                    │
│ [Cancelar]        [Adicionar às refs] │
└───────────────────────────────────────┘
```

Confirmar chama `addImageCapture(uri, category = "Referência")`; cancelar/fechar não persiste. **Atenção:** o texto explicativo atual ainda menciona anexar a uma nova tarefa; corrigir essa string junto da nomenclatura da notificação.

## 4. Detalhe fullscreen

```text
┌─────────────────────────────────────────┐
│ Título completo          [☷ subtarefas]│
│ Data/hora · [Categoria]             [✕]│
├─────────────────────────────────────────┤
│                                         │
│              IMAGEM INTEGRAL            │
│             ContentScale.Fit            │
│                                         │
│       ‹ Deslize para navegar ›          │
└─────────────────────────────────────────┘
```

- Imagem ocupa o espaço disponível, sem recorte.
- A dica de swipe é uma camada visual alinhada ao centro inferior sobre a imagem; não participa da medição do layout.
- Swipe-left navega para a próxima referência; swipe-right para a anterior.
- Usa a lista de referências recebida pelo cartão; sem loop nas extremidades.
- Atalho de sub-checklist e botão fechar são controles separados do gesto.

## 5. Sub-checklist

```text
┌───────────────────────────────────────┐
│ Sub-checklist · 2/4 concluídas      [X]│
├───────────────────────────────────────┤
│ ☑ Item concluído                   [🗑]│
│ □ Próximo item                     [🗑]│
│ ┌─────────────────────────────┐ [+]   │
│ │ Nova subtarefa              │       │
│ └─────────────────────────────┘       │
└───────────────────────────────────────┘
```

Persistência em `capture_subtasks`; cada item pertence a uma referência via `captureId`.

## 6. Tema e estados
- Tema escolhido: seguir sistema, claro ou escuro.
- Tema atual usa paleta azul-marinho/oceano/ciano.
- Estados previstos: sem referências, busca sem resultado, imagem ausente, OCR processando, OCR sem texto/erro, referência concluída.
- Validar com fontes ampliadas, leitores de tela, largura reduzida e dispositivos reais.


### Detalhe fullscreen (atualização 2026-10-29)

- O conteúdo principal é um pager horizontal; deslizar à esquerda avança e à direita volta dentro da lista filtrada.
- O cabeçalho exibe a ação **Editar título**. O diálogo não aparece no cartão/lista e não substitui o controle de sub-checklist do cartão.
- A dica de navegação fica sobreposta à imagem; a imagem mantém proporção com `ContentScale.Fit`.


## Fullscreen details — current behavior

- Horizontal pager: swipe left to next reference, swipe right to previous; filtered ordering, no wrap-around.
- Header includes **Editar título**, shown only in fullscreen.
- Sub-checklist remains available from the reference card, not from the fullscreen header.
- Image uses `ContentScale.Fit`; navigation hint remains an overlay.
