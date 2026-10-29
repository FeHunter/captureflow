# Pesquisa de apps populares e heurísticas de OCR por interface

**Data da pesquisa:** 29/10/2026  
**Objetivo:** melhorar a seleção do título de screenshots compartilhadas para CaptureFlow, sem depender de uma lista rígida de palavras por app.

## Resumo executivo

Não existe uma única classificação pública e comparável que forneça os 100 apps mais usados em cada um dos anos de 2024, 2025 e 2026, com a mesma métrica, cobertura geográfica e metodologia. Algumas fontes medem usuários ativos mensais (MAU), outras downloads, instalações estimadas ou abertura de apps. Não devem ser tratadas como sinônimos.

Para evitar apresentar números incompatíveis como se fossem uma única tabela:
- Para 2024, o relatório Sensor Tower State of Mobile 2025 publica um ranking mundial dos principais apps por MAU; entre os dez primeiros estão YouTube, Chrome, Facebook, WhatsApp, Google, Instagram, Gmail, TikTok, Messenger e Google Photos.
- Para 2025, a Udonis publica uma lista de 100 apps por usuários mensais estimados. A lista é uma referência útil para cobertura, mas não é uma métrica diretamente intercambiável com o ranking de downloads da AppMagic/AppTweak ou com os índices de uso da Similarweb.
- Para 2026, em 29/10/2026, o ano ainda não terminou. Rankings anuais completos não devem ser apresentados como finais. O State of Mobile 2026 cobre sobretudo resultados de 2025; rankings mensais de 2026 são snapshots, não um ranking anual fechado.

## Referências de popularidade

### Uso/MAU — referências principais

1. [Sensor Tower — State of Mobile 2025, rankings mundiais de 2024](https://sensortower.com/press/sensor-tower-consumer-spending-on-apps-soared-in-2024-and-mobile-rode-the-wave-of-ai-enthusiasm) — fonte de indústria com métricas separadas para downloads, receita e MAU.
2. [PDF do relatório State of Mobile 2025 — rankings de 2024](https://elements.visualcapitalist.com/wp-content/uploads/2025/01/sensor_tower__state_of_mobile_2025__en.pdf) — ranking mundial de MAU: YouTube, Chrome Browser, Facebook, WhatsApp Messenger, Google, Instagram, Gmail, TikTok, Facebook Messenger e Google Photos.
3. [Udonis — 100 Most Used Apps by Monthly Users](https://www.blog.udonis.co/mobile-marketing/mobile-apps/most-used-apps) — lista de 100 apps estimados por usuários mensais em 2025 e uma lista separada para os EUA. É a lista completa de 100 usada como catálogo de cobertura inicial, não como verdade universal para os outros anos.
4. [DataReportal — Digital 2026 Mid-Year Global Update](https://datareportal.com/reports/digital-2026-mid-year-global-update-report) — apresenta índices de uso real de apps baseados em dados de abertura/atividade; a própria fonte explica que o índice não é uma contagem absoluta de pessoas.
5. [Sensor Tower — State of Mobile 2026](https://sensortower.com/blog/state-of-mobile-2026) — analisa resultados de mercado de 2025 publicados em 2026; não equivale a um ranking fechado de uso durante todo o ano de 2026.

### Downloads — usados apenas como sinal complementar de cobertura

- [AppTweak — apps mais baixados globalmente em 2025](https://www.apptweak.com/en/reports/most-downloaded-apps-globally-android-ios): dados de janeiro a dezembro de 2025, App Store + Google Play.
- [Udonis — downloads globais por ano](https://www.blog.udonis.co/mobile-marketing/mobile-apps/most-downloaded-mobile-apps): lista histórica de downloads e rankings por categoria.
- [Appfigures — leaderboards](https://appfigures.com/reports/leaderboards): permite consultar rankings por país, plataforma e período; os rankings são atualizados e, portanto, snapshots mudam.

**Limitação da pesquisa:** não foi encontrada uma fonte pública aberta que forneça, de forma consistente, os 100 apps mais usados por MAU para cada um dos três anos. Não preenchemos as lacunas com rankings de downloads, nem chamamos um ranking parcial de 2026 de anual definitivo.

## Padrões de interface a cobrir

O catálogo de popularidade inclui redes sociais, vídeo curto e longo, mensageria, navegador, notícias, compras, música/streaming, produtividade/IA, mapas/viagens e utilitários. Para OCR, esses grupos são mais úteis do que tentar manter regras exclusivas para cada um dos 100 nomes.

| Família visual | Exemplos de apps populares | Onde o título costuma aparecer | Ruído comum |
|---|---|---|---|
| Vídeo/streaming | YouTube, TikTok, Instagram Reels, Facebook Watch, Netflix | Abaixo do player, sobre o vídeo ou em legenda | barra do navegador, Open App, views/likes, Subscribe, Shorts, comentários, botões de ação |
| Feed social | Instagram, Facebook, Threads, X, Pinterest, Reddit, Snapchat | Texto principal da publicação, legenda ou cabeçalho do post | handle (@), ações, contagem de curtidas, tabs, navegação inferior |
| Notícias/leitura | BBC, portais de notícias, Medium, Reddit | Manchete destacada, geralmente próxima do topo do artigo | URL, logo, data, autor, publicidade, navegação |
| Comércio | Amazon, Temu, SHEIN, Mercado Livre, Shopee, AliExpress | Nome do produto próximo à imagem/preço | preço, desconto, frete, avaliações, botões de compra, recomendados |
| Produtividade/IA | ChatGPT, Gmail, Drive, Docs, Notion, Outlook | título da conversa/documento ou primeira mensagem significativa | nome do app, ações de menu, placeholders, barra de ferramentas |
| Música/áudio | Spotify, YouTube Music, SoundCloud | título da faixa/playlist próximo à capa | artista, duração, controles, recomendações |
| Navegador/web | Chrome e navegadores móveis | título da página dentro do conteúdo; em artigos, manchete | URL e controles do browser podem ser maiores ou mais largos que o título |
| Mensageria | WhatsApp, Telegram, Messenger, WeChat, LINE | título do grupo/contato ou conteúdo da mensagem selecionada | nome do contato, timestamps, status, caixas de composição e ações |

Os exemplos acima são classes de layout, não uma alegação de que todos os apps da classe usam exatamente o mesmo design. O UI muda por plataforma, idioma, versão, região, conta, anúncios e experimento A/B.

## Heurísticas implementadas

1. **Detectar uma família visual aproximada** a partir de sinais do texto OCR: vídeo, social, notícia, comércio, produtividade ou genérica.
2. **Pontuar candidatos** por altura da caixa, largura relativa, comprimento semelhante a uma manchete, proporção de palavras e posição vertical relativa à família.
3. **Filtrar ruído forte** como URLs, domínios, horários isolados, Open App, youtube.com/watch, handles e linhas dominadas por contagens de likes/views/comments.
4. **Evitar palavras proibidas genéricas em excesso.** Uma palavra como “share”, “views” ou “more” pode fazer parte de um título real; a filtragem deve preferir combinações de sinais de interface.
5. **Agrupar linhas próximas** com altura semelhante para formar títulos multilinha.
6. **Manter fallback genérico.** Se nenhum perfil for reconhecido, o OCR usa a heurística geométrica geral em vez de falhar.

### Casos de regressão

- BBC: Luxury cruise passengers stranded in Cambodia due to soaring fuel costs.
- Visual Capitalist: Mapped: Unemployment Rates Around the World in 2026.
- YouTube mobile: título começando com “10 Easy craft ideas / miniature craft / Paper ...”; não selecionar m.youtube.com/watch, Open App ou metadados de engajamento.
- Testar também título com palavras como “views” ou “share” no texto real para evitar falso descarte.

## Limites e próximos passos

- ML Kit Text Recognition não fornece uma garantia universal de peso tipográfico/bold; a implementação aproxima a proeminência por geometria e forma textual.
- Os perfis são heurísticos, não reconhecimento oficial de app nem visão semântica.
- O OCR precisa ser validado com screenshots reais de várias versões, idiomas e layouts.
- Próxima evolução recomendada: manter uma coleção de screenshots de regressão licenciadas/fornecidas pelo usuário e comparar o título esperado com o resultado real em testes automatizados, sem enviar imagens a serviços externos.
