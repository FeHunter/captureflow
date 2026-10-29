# Wireframes — Biblioteca, grade de cartões e busca

## Objetivo

Este wireframe usa como referência visual o anexo captureflow-card-layout.html. Ele especifica a composição da biblioteca, o campo de busca e a grade de cartões para implementação em Android nativo com Kotlin, Jetpack Compose e Material 3.

O anexo define uma biblioteca em **duas colunas simétricas** e cartões de dimensões consistentes. A validação final deve ocorrer em dispositivo e nos temas claro e escuro.

## 1. Biblioteca — estrutura da tela

~~~text
┌──────────────────────────────────────┐
│ Biblioteca                       ⋮   │
│ ┌──────────────────────────────────┐ │
│ │ 🔍  Pesquisar…                   │ │
│ └──────────────────────────────────┘ │
│ [Todas] [Ler depois] [Ideias] [Tarefa]│
│ [Importante] [Mais tarde]            │
│ ┌───────────────┐ ┌───────────────┐  │
│ │   Miniatura   │ │   Miniatura   │  │
│ ├───────────────┤ ├───────────────┤  │
│ │ Título        │ │ Título        │  │
│ │ Título comp…  │ │ Descrição…    │  │
│ │ [Categoria ▾] │ │ [Categoria ▾] │  │
│ │ Data · hora   │ │ Data · hora   │  │
│ │ □   ☰ 2/4  🗑 │ │ □   🔍     🗑 │  │
│ └───────────────┘ └───────────────┘  │
│ ┌───────────────┐ ┌───────────────┐  │
│ │   Miniatura   │ │ Sem imagem    │  │
│ ├───────────────┤ ├───────────────┤  │
│ │ Título        │ │ Título        │  │
│ │ Título comp…  │ │ Descrição…    │  │
│ │ [Categoria ▾] │ │ [Categoria ▾] │  │
│ │ Data · hora   │ │ Data · hora   │  │
│ │ □   ☰ 0/0  🗑 │ │ ☑   🔍     🗑 │  │
│ └───────────────┘ └───────────────┘  │
└──────────────────────────────────────┘
~~~

A grade continua verticalmente rolável. A busca e os filtros ficam acima da grade.

## 2. Campo de busca

~~~text
┌──────────────────────────────────────┐
│ 🔍  Pesquisar…                    ✕  │
└──────────────────────────────────────┘
~~~

- Campo de largura disponível, alinhado às margens da tela.
- Ícone de lupa à esquerda e ação para limpar o texto à direita quando houver conteúdo.
- Placeholder: **“Pesquisar…”**.
- Superfície alternativa do tema atual, cantos arredondados e contraste legível.
- Filtrar os cartões enquanto o usuário digita, conforme os campos pesquisáveis já suportados pela feature.
- Ao limpar a busca, restaurar os resultados de acordo com o filtro de categoria selecionado.
- Manter estado visual de foco e rótulo acessível.
- Ocultar o botão de limpar quando o campo estiver vazio.
- Sem correspondências: apresentar estado vazio claro e permitir editar ou limpar a busca.

O anexo não define os campos exatos pesquisáveis nem a estratégia de debounce. Esses detalhes devem seguir a implementação existente, sem inventar critérios neste wireframe.

## 3. Filtros de categoria

Os filtros aparecem logo abaixo da busca como chips compactos. A lista pode rolar horizontalmente se não couber na largura disponível.

Ordem e rótulos:
- **Todas** — selecionado por padrão.
- **Ler depois**
- **Ideias**
- **Tarefa**
- **Importante**
- **Mais tarde**

O chip selecionado usa a cor primária do tema; os demais usam contorno e texto secundário. “Todas” é um filtro, não uma categoria atribuída ao cartão.

Busca e filtro devem funcionar em conjunto: alterar o filtro não limpa a busca, e limpar a busca não redefine o filtro.

## 4. Grade e dimensões

- Duas colunas simétricas em dispositivos Android de largura comum.
- Referência para tela de 360 dp: padding lateral de 16 dp, espaçamento de 12 dp e cartões com aproximadamente 164 dp de largura.
- Altura de referência do cartão: **300 dp**, para acomodar o subtítulo em até duas linhas com espaçamento confortável.
- Implementação sugerida: LazyVerticalGrid com GridCells.Fixed(2).
- Espaçamento vertical e horizontal consistente de 12 dp.
- O conteúdo variável não deve provocar sobreposição.
- Em telas muito estreitas ou com escala de fonte elevada, adaptar a composição para preservar legibilidade e alvos de toque.
- Não criar rolagem interna dentro do cartão.

## 5. Anatomia do cartão

~~~text
┌────────────────────────┐
│ Miniatura + ↗ expandir │
├────────────────────────┤
│ Título principal       │  38–42 dp / até 2 linhas
│ Subtítulo              │  28–34 dp / até 2 linhas
│ [Tag/categoria ▾]      │  chip compacto
│ 29/10/2026 · 14:35     │  data e hora
├────────────────────────┤
│ □       ☰ 2/4      🗑  │  ações / 48 dp
└────────────────────────┘
~~~

| Área | Referência | Regra |
|---|---:|---|
| Miniatura | 104 dp | Imagem com recorte consistente; placeholder equivalente sem imagem; faixa OCR/erro quando necessário |
| Título principal | 38–42 dp | Até 2 linhas; tocável para abrir detalhes |
| Texto secundário | 28–34 dp | Até 2 linhas, com folga vertical; tocável para abrir detalhes |
| Tag e data/hora | cerca de 44 dp | Chip compacto e data/hora derivada de createdAt |
| Ações | 48 dp | Alvos de toque de 48 × 48 dp; conclusão, sub-checklist/analisar e excluir |

Data e hora devem seguir o formato dd/MM/yyyy · HH:mm, usando o fuso local do dispositivo e o timestamp persistido.

### Cartão de referência

- Miniatura de capa, título, texto secundário quando necessário, tag, data/hora e ações.
- A ação de sub-checklist apresenta progresso, por exemplo **“☰ 2/4”**.
- A sub-checklist abre em bottom sheet, não dentro do cartão.
- Não exibir o texto OCR bruto.
- A análise OCR inicia automaticamente conforme o fluxo existente; mostrar “Analisando imagem…” ou erro em uma faixa discreta na miniatura.

### Cartão de tarefa

- Mesma linguagem visual, largura e altura do cartão de referência.
- Título, descrição resumida opcional, tag, data/hora, conclusão e exclusão.
- Mostrar a ação de analisar screenshot quando houver imagem e a ação estiver disponível.
- Em itens concluídos, combinar checkbox marcado com texto atenuado/riscado; não depender somente da cor.

### Sem imagem, erro e conclusão

- Sem imagem: manter o slot de 104 dp com placeholder e ícone.
- Erro de imagem/OCR: manter dados e ações, com aviso curto.
- Concluído: checkbox marcado e título riscado/atenuado, preservando contraste.
- Título longo: limitar o principal a duas linhas; mostrar título secundário em uma linha truncada e título integral no detalhe/tela cheia.
- Tag longa: limitar a uma linha e usar reticências sem deslocar as ações.

## 6. Interações complementares

### Sub-checklist

~~~text
┌──────────────────────────────────┐
│ Sub-checklist · 2/4 concluídas  ✕│
├──────────────────────────────────┤
│ ☑ Definir escopo              🗑 │
│ ☑ Revisar tokens              🗑 │
│ □ Estimar                     🗑 │
│ □ Publicar                    🗑 │
│ ┌──────────────────────────┐ [+] │
│ │ Nova subtarefa           │     │
│ └──────────────────────────┘     │
└──────────────────────────────────┘
~~~

Abrir como modal bottom sheet ao tocar no botão de sub-checklist. O botão fica sempre visível entre concluir e excluir, inclusive quando o progresso é 0/0, permitindo criar a primeira subtarefa. Permitir marcar, excluir e adicionar subtarefas sem alterar a altura do cartão na grade.

### Visualização da imagem

Ao tocar na miniatura ou no título, abrir detalhes em tela cheia. Exibir ícone de expansão no canto superior da miniatura. A tela de detalhes mostra título completo, data/hora, categoria, imagem inteira com ajuste fit/contain quando disponível e conteúdo capturado quando houver. O controle de fechar deve ter alvo mínimo de 48 dp.

## 7. Temas e paleta

Manter o tema claro/escuro já utilizado no app.

| Uso | Tema claro | Tema escuro |
|---|---|---|
| Fundo | #F4F8FC | #07182B |
| Superfície/cartão | #FFFFFF | #0C2B46 |
| Texto principal | #07182B | #F3F7FB |
| Texto secundário | #3D5D73 | #B7CEDF |
| Primária/ações | #1F5F8B | #63C4F0 |
| Superfície alternativa | #E1EDF6 | #164B62 |
| Contorno | #7D9AAF | #52758E |

A combinação violeta #F7F5FF / #29213D é apenas referência visual complementar, não substitui automaticamente a paleta ativa.

## 8. Critérios de aceite

- [ ] Campo de busca aparece antes dos filtros e da grade, com lupa, placeholder e limpeza condicional.
- [ ] Busca e filtro de categoria podem ser combinados.
- [ ] Filtros usam os rótulos definidos e destacam o item selecionado.
- [ ] A biblioteca apresenta duas colunas simétricas em larguras comuns.
- [ ] Cartões de referência e tarefa compartilham dimensões e slots visuais.
- [ ] Imagem, ausência de imagem, OCR em processamento, erro e conclusão não quebram a grade.
- [ ] Data/hora vem de createdAt em formato local consistente.
- [ ] Ações têm alvos de toque adequados e rótulos acessíveis.
- [ ] O botão de sub-checklist fica entre concluir e excluir e abre modal bottom sheet, inclusive quando ainda não há subtarefas.
- [ ] Tocar na imagem ou no título abre detalhes em tela cheia com ícone de expansão visível.
- [ ] O subtítulo recebe altura suficiente para até duas linhas sem ficar comprimido.
- [ ] Tema claro e escuro mantêm contraste e usam a paleta atual.
