# CaptureFlow — Personal Notebook Design System

**Status:** aprovado para implementação  
**Direção:** Hybrid Personal Notebook  
**Temas:** claro, escuro e seguir sistema  
**Plataforma canônica:** Android nativo, Kotlin, Jetpack Compose e Material 3  
**Escopo:** rebrand visual e reorganização de apresentação, sem mudanças funcionais.

## 1. Princípio do produto

CaptureFlow é um espaço pessoal para organizar referências, pensamentos, ideias e pequenas tarefas vinculadas a essas referências. A experiência deve lembrar um caderno pessoal/arquivo de fichas acolhedor, sem se transformar em um planner ou dashboard corporativo.

Pilares:
- **Acolhimento:** cores quentes, contraste confortável e pouco ruído visual.
- **Intimidade:** referências sutis a papel, fichas, etiquetas e escrita manual.
- **Organização intuitiva:** hierarquia clara, navegação previsível e ações reconhecíveis.

Direção híbrida: interface digital moderna como base, linguagem de papelaria nos cards/superfícies e detalhes manuscritos pontuais. A escrita manual nunca deve prejudicar leitura, acessibilidade ou identificação de ações.

## 2. Tokens de cor

### Light
| Token | Hex | Uso |
|---|---|---|
| background | #F3EBDD | Fundo geral |
| surface | #FFFCF5 | Cards e superfícies |
| surfaceVariant | #EAE0CE | Superfícies secundárias |
| primary | #9A7138 | Ações principais |
| secondary | #7C8063 | Destaques e conclusão |
| tertiary/accent | #C77A48 | Acentos pontuais |
| onBackground/onSurface | #39382F | Texto principal |
| onSurfaceVariant | #756F61 | Texto auxiliar |
| outline/outlineVariant | #D8CCB8 | Contornos e divisores |
| primaryContainer | #E8D9BE | Superfície de ação |
| onPrimaryContainer | #39382F | Texto sobre ação suave |

### Dark
| Token | Hex | Uso |
|---|---|---|
| background | #292A27 | Fundo geral |
| surface | #343630 | Cards e superfícies |
| surfaceVariant | #41433A | Superfícies secundárias |
| primary | #D5B47A | Ações principais |
| secondary | #B6B991 | Destaques e conclusão |
| tertiary/accent | #D99163 | Acentos pontuais |
| onBackground/onSurface | #F0E8D9 | Texto principal |
| onSurfaceVariant | #C2B9A9 | Texto auxiliar |
| outline/outlineVariant | #55564C | Contornos e divisores |
| primaryContainer | #51432F | Superfície de ação |
| onPrimaryContainer | #F0E8D9 | Texto sobre ação suave |

Regras:
- Dark não é inversão literal de Light.
- Evitar preto/branco puros como superfícies predominantes.
- Status de erro, aviso e sucesso devem ser distinguíveis também por texto/ícone, não apenas cor.
- Contraste deve ser validado em dispositivos reais; ajustar tokens se necessário sem descaracterizar a paleta.
- Categorias e tags existentes não ganham novos valores por causa do rebrand.

## 3. Tipografia

- **Interface e conteúdo:** sans-serif legível (Inter como direção tipográfica; fallback nativo sans-serif se o asset ainda não estiver incluído).
- **Títulos editoriais:** serif (Lora como direção; fallback nativo serif).
- **Detalhes manuscritos:** Caveat/cursive apenas em pequenos detalhes decorativos; nunca em conteúdo longo, controles, labels funcionais ou texto de baixa legibilidade.
- Não introduzir downloads de fontes em runtime. Se fontes customizadas forem adicionadas, incluir os arquivos licenciados no app e documentar origem/licença.

Escala inicial (sp):
| Token | Tamanho | Uso |
|---|---:|---|
| display | 30 | Títulos especiais |
| h1 | 24 | Cabeçalhos |
| h2 | 20 | Seções |
| h3 | 18 | Títulos de cards |
| body | 16 | Conteúdo |
| bodySmall | 14 | Informação secundária |
| caption | 12 | Metadados |

A escala é uma referência; preservar adaptações de acessibilidade e fonte ampliada do sistema.

## 4. Espaçamento, formas e elevação

Espaçamento base 4 dp: 4, 8, 12, 16, 24, 32 e 48 dp.
- Campos: raio 12 dp.
- Cards: raio 16 dp.
- Modais: raio 20–24 dp.
- Botões: raio 12 dp.
- Tags: 8 dp ou pill, conforme o componente existente.
- Área de toque recomendada: mínimo 48 dp.
- Sombras discretas no tema claro; no escuro priorizar contraste entre superfícies.
- Textura de papel, se aplicada, deve ser muito sutil, leve e não repetir bitmaps grandes por componente.

## 5. Componentes e comportamento visual

Padronizar por tokens e componentes reutilizáveis:
- Cards/fichas de referência: capa, título, tags e ações atuais; não alterar ações nem conteúdo persistido.
- Campos de busca e filtros: estados de foco, limpeza e filtro selecionado visíveis.
- Tags: usar exclusivamente os valores atuais Ler depois, Verificar, Salvar e Importante; Todas é filtro, não tag persistida.
- Checkbox e checklist: feedback claro de concluído/pendente.
- Notes: superfície acolhedora para blocos de texto/imagem; preservar edição, remoção, recorte, compressão e limites.
- Modais e menus: hierarquia e ações atuais preservadas.
- Estados vazios, carregamento e erro: mensagens claras e acolhedoras, sem alterar condições ou eventos.
- Navegação e ícones: manter destinos, affordances, descrições acessíveis e callbacks atuais.

Não trocar ícones funcionais por ilustrações manuscritas. Não esconder ações existentes em nome do minimalismo.

## 6. Diretrizes por tela

### Biblioteca
Preservar a tela principal de referências, estrutura de categorias, busca, filtros, resumo de progresso, cards/containers, thumbnails, ordenação, controle de concluídos, entrada de imagem/câmera e acesso a Newsletters. Não introduzir bottom navigation ou tela autônoma de tarefas.

### Fullscreen
Preservar título imediatamente abaixo do header, data/categoria, link original, mídia e capa, pager/swipe, badge de concluído, ações, notas, sub-checklist e edição de título/categoria. O título nunca pode ser removido ou deslocado para uma posição que prejudique sua leitura.

### Notes
Preservar blocos ordenados de texto e imagem, criar/editar/remover, expansão de imagem, recorte, cancelamento e salvamento. O modelo e persistência continuam pertencendo a Captures.

### Categorias e Dashboard
Preservar CRUD e validações de categorias, filtros, reatribuição segura, contagens, progresso e expansão/recolhimento. Não criar categoria Concluído.

### Newsletters
Preservar navegação, gerenciamento de fontes RSS/Atom, atualização, estados de leitura, leitor, abrir original e salvamento explícito na Biblioteca.

### Settings
Preservar tema system/light/dark, ordenações configuradas e perfil de compressão. O tema deve continuar respeitando preferência persistida e modo do sistema.

### Fluxos transversais
Preservar Sharesheet, Photo Picker, câmera, OCR local, metadados, compressão, armazenamento privado, monitoramento opt-in de screenshots, notificações, player YouTube, links, permissões e privacidade.

## 7. Contrato de não regressão — funcionalidades protegidas

O rebrand é apresentação-only. É proibido remover, adicionar, renomear ou alterar sem aprovação explícita:
- Funcionalidades, ações, destinos, gestos, callbacks e estados de UI que representem comportamento de produto.
- Modelos de domínio, contratos de repositório, UseCases, ViewModels e eventos de negócio.
- Schema Room, migrations, preferências persistidas e formatos serializados.
- Integrações Android, intents, permissões, notificações, WebView/IFrame, OCR, metadados, compressão e armazenamento.
- Regras de categorias, tags, filtros, busca, ordenação, conclusão e deduplicação.
- Fluxos de Notes, imagens múltiplas, sub-checklists e Newsletters.

Antes de editar uma tela:
1. Ler o código e a documentação específica da feature.
2. Registrar seus estados, entradas, ações, saídas, navegação, gestos, callbacks e dependências.
3. Separar mudanças visuais de mudanças comportamentais.
4. Alterar apenas Presentation/tema/componentes, salvo necessidade técnica demonstrada.
5. Comparar o fluxo antes/depois e atualizar testes visuais/UI quando existentes.
6. Executar testes relevantes, `testDebugUnitTest` e `assembleDebug`.
7. Verificar que nenhum contrato de domínio/data ou preferência persistida foi alterado.

Se documentação histórica divergir do código atual, não escolher silenciosamente uma versão: investigar o código e contratos mais recentes, registrar a divergência e preservar o comportamento atualmente implementado.

O checklist detalhado por fluxo está em [docs/rebrand-regression-checklist.md](rebrand-regression-checklist.md) e deve ser executado como parte da revisão de regressão. A documentação de estado atual continua sendo referência complementar, sempre confrontada com o código.

## 8. Sequência de implementação

1. Inventário de funcionalidades e baseline de testes/build.
2. Tokens, temas Light/Dark e tipografia.
3. Componentes compartilhados.
4. Biblioteca e navegação.
5. Fullscreen e Notes.
6. Categorias, Dashboard, Newsletters e Settings.
7. Fluxos de captura/importação e diálogos.
8. Revisão visual, acessibilidade, regressão e CI.

Cada etapa deve ser revisada antes da próxima. Não combinar redesign com refatoração arquitetural ou mudanças de produto.

## 9. Critérios de aceite

- Tema claro, escuro e seguir sistema funcionam e persistem como antes.
- Todos os fluxos e funcionalidades existentes continuam acessíveis e operacionais.
- Nenhuma alteração não aprovada em Domain, Data, Room, migrations, intents ou preferências.
- Layout legível em telas estreitas, fonte ampliada e estados com conteúdo longo.
- Testes unitários/build aprovados no commit candidato.
- Documentação da feature e `AGENTS.md` atualizados.
- Comparação visual antes/depois registrada por tela.


## 10. Auditoria e aplicação visual — 2026-11-04

A revisão das views identificou que a primeira etapa havia atualizado principalmente o tema global, deixando superfícies locais com gradientes e cores azuis hardcoded. Esta rodada corrige os pontos de maior impacto e estabelece a linguagem compartilhada:

| View / feature | Aplicação |
|---|---|
| Biblioteca / categorias | Cards em formato de ficha, cabeçalho com cor suave, contagem pendente/concluída, progresso e ação Ver todas |
| Dashboard / Meu progresso | Painel de caderno com resumo total e fichas de progresso por categoria |
| Thumbnails e status | Badges de concluído com cores semânticas e contornos suaves |
| Filtros de categoria | Etiquetas com estado selecionado em papel colorido |
| Categoria expandida | Cabeçalho aquecido e filtros Pendentes/Concluídas alinhados ao tema |
| Notes | Cabeçalho de caderno e área de escrita pautada com margem lateral |
| Fullscreen | Header e título em superfície de papel; mídia continua sobre fundo escuro; dock de ações com superfície quente |
| Newsletters | Cards editoriais com bordas suaves e leitor com tipografia/cores quentes |
| Captura e recorte | Tela de recorte com cabeçalho editorial; viewfinder da câmera continua escura por legibilidade |
| Settings / navegação | App bar e menus usam tokens semânticos do tema |

O componente compartilhado `NotebookPaper` está disponível em `core/ui/components/NotebookPaper.kt` e já é utilizado no Dashboard. A textura pautada é aplicada somente ao campo de escrita de Notes, não em imagens, vídeos ou controles gerais.

A implementação preserva os callbacks, os dados, os filtros, as ordenações, os fluxos de navegação, o armazenamento e os contratos funcionais. A conclusão de regressão ainda depende de CI e validação visual em dispositivo físico/emulador.
