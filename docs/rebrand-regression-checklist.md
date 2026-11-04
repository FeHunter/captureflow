# CaptureFlow — Rebrand Functional Regression Checklist

Este inventário é o contrato de regressão para o rebrand **Hybrid Personal Notebook**. A mudança visual só pode ser considerada concluída quando os fluxos abaixo continuarem disponíveis e com o mesmo comportamento do baseline.

**Regra:** checklist não autoriza mudanças funcionais. Se o código atual divergir de uma descrição histórica, investigar e registrar a divergência antes de alterar qualquer comportamento.

## 1. Biblioteca e organização

- [ ] A tela principal continua sendo a Biblioteca de referências; não criar bottom navigation ou uma tela autônoma de tarefas.
- [ ] Cards/containers de categoria mantêm suas ações e conteúdo atuais.
- [ ] Os containers mantêm prévias de até três referências e a ação de abrir/ver todas.
- [ ] A categoria expandida mantém grade de thumbnails e alternância entre pendentes/concluídas.
- [ ] Busca continua filtrando título/conteúdo e respeitando a categoria selecionada.
- [ ] O botão de limpar busca continua disponível quando aplicável.
- [ ] Filtros de categoria continuam incluindo as categorias cadastradas e a opção Todas (filtro, não valor persistido).
- [ ] O filtro de categorias mantém a apresentação rolável em duas linhas e o modal Ver todas as categorias.
- [ ] CRUD de categorias, validações e reatribuição segura de referências ao excluir categoria continuam funcionando.
- [ ] Ocultar/mostrar referências concluídas continua funcionando.
- [ ] Conclusão permanece estado independente da categoria; concluir/reabrir não altera a categoria.
- [ ] O selo CompletedBadge continua identificando referências concluídas nos locais previstos.
- [ ] Meu progresso mantém total concluído, progresso por categoria, contagens pendentes/concluídas e expansão/recolhimento da lista.
- [ ] Ordenação de containers de categoria mantém ordem manual/padrão, mais recentes, mais antigas, A–Z e Z–A, com os critérios temporais já implementados.
- [ ] A ordenação de containers não altera a ordem natural das prévias; a grade expandida mantém sua ordenação padrão atual.
- [ ] A entrada de Newsletters continua acessível pela navegação existente.

## 2. Captura e importação

- [ ] ACTION_SEND e ACTION_SEND_MULTIPLE continuam aceitando texto, URLs e imagens.
- [ ] Links compartilhados continuam pendentes até escolha explícita de categoria e confirmação.
- [ ] Cancelar/fechar a confirmação de link não persiste a referência.
- [ ] Extração de metadados continua fora da main thread, com timeouts, limites de resposta e fallback para o link original.
- [ ] YouTube continua priorizando oEmbed para título/thumbnail e mantendo fallback de metadados.
- [ ] Demais páginas continuam priorizando H1 e imagem de conteúdo, com metadados sociais/document title/host como fallback.
- [ ] URLs relativas e redirects continuam sendo tratados.
- [ ] Falha de rede/metadados não descarta o link; título fallback continua permitindo salvar.
- [ ] Deduplicação de links e textos continua conforme regras existentes; imagens distintas não são deduplicadas por placeholder genérico.
- [ ] Photo Picker continua permitindo importar imagens.
- [ ] Câmera continua permitindo foco por toque, captura, revisão e recorte nas proporções suportadas.
- [ ] A revisão de imagem continua permitindo editar título e escolher categoria antes de salvar.
- [ ] OCR continua on-device, automático e sem persistir OCR bruto como conteúdo principal.
- [ ] Falha de OCR não elimina a referência/imagem.
- [ ] Imagem continua sendo armazenada como capa quando aplicável.

## 3. Compressão e armazenamento de imagens

- [ ] Perfil Médio continua usando JPEG quality 70.
- [ ] Perfil Alto continua usando JPEG quality 50.
- [ ] Limite de dimensão, correção de orientação EXIF e normalização permanecem como implementados.
- [ ] URIs file:// e content:// continuam sendo lidas pelos caminhos apropriados, com fallback de descritor quando necessário.
- [ ] Falha de compressão mantém a URI/imagem original disponível como fallback conforme o fluxo existente.
- [ ] Imagens compartilhadas são copiadas para armazenamento privado quando possível.
- [ ] Falha na cópia não perde a referência e mantém o fallback de URI original.
- [ ] Imagens escolhidas manualmente nunca são excluídas pelo app.
- [ ] Não introduzir upload ou sincronização cloud.

## 4. Screenshots e permissões

- [ ] Monitoramento de screenshots continua opcional e opt-in.
- [ ] Serviço/notificação persistente continua permitindo ativar/desativar monitoramento.
- [ ] Detecção continua best-effort via MediaStore/metadados; não introduzir Accessibility Service ou leitura de pixels de outros apps.
- [ ] Notificação continua encaminhando a URI correta e abrindo confirmação em cold start e quando app está aberto.
- [ ] Salvar screenshot exige escolha de categoria e confirmação explícita.
- [ ] Screenshot original só pode ser excluída após cópia privada bem-sucedida no fluxo autorizado.
- [ ] Falha de exclusão da origem continua não fatal.
- [ ] Permissões de imagens e notificações continuam respeitando a versão Android.

## 5. Fullscreen e mídia

- [ ] O título da referência continua imediatamente abaixo do header.
- [ ] Data, categoria e atalho para URL original continuam visíveis conforme as condições atuais.
- [ ] Imagem continua sendo exibida sem distorção/recorte indevido.
- [ ] Pager horizontal continua navegando entre referências da mesma categoria e mesmo estado de conclusão, sem loop nas extremidades.
- [ ] A página estabilizada continua sincronizando título, data, categoria, mídia e conclusão.
- [ ] Navegação entre múltiplas imagens por referência e seleção de capa continuam funcionando.
- [ ] O limite atual de até 10 mídias por referência permanece.
- [ ] Badge de concluído permanece sobreposto à imagem.
- [ ] Ações atuais continuam acessíveis: abrir link, concluir/reabrir, editar título/categoria, abrir Notes, sub-checklist, excluir e fechar, respeitando os estados atuais.
- [ ] Back fecha apenas a camada fullscreen e mantém o estado anterior da Biblioteca.
- [ ] Vídeos YouTube válidos continuam usando a feature youtubeplayer isolada, WebView/IFrame, sem autoplay e com link original/retry em falhas.
- [ ] Não alterar headers/referer, origin, lifecycle, callbacks, segurança ou política de navegação do player durante o rebrand.

## 6. Notes e sub-checklist

- [ ] Notes continua pertencendo ao agregado Capture e sendo persistido no formato versionado atual, com leitura retrocompatível.
- [ ] Blocos ordenados de texto e imagem continuam suportados.
- [ ] Limite de texto de 10.000 caracteres continua.
- [ ] Criar, editar e excluir bloco continuam funcionando.
- [ ] Toque longo continua abrindo edição/remoção do bloco; toque simples na imagem continua expandindo-a.
- [ ] Recorte continua oferecendo Original, 1:1, 4:5 e 16:9.
- [ ] Recorte continua sendo codificado em PNG lossless em memória e comprimido em JPEG no perfil Alto, sem arquivo/URI intermediária.
- [ ] Falha no recorte/compressão mantém seleção e prévia para nova tentativa.
- [ ] Cancelar não persiste alterações e limpa apenas imagens temporárias geradas pelo fluxo.
- [ ] Sub-checklist continua vinculada ao captureId e persistida em capture_subtasks.
- [ ] Adicionar, concluir e remover subtarefas continuam funcionando no fullscreen.
- [ ] Não reintroduzir tela independente de Tasks ou a tabela legada checklist_tasks como feature ativa.

## 7. Newsletters

- [ ] Entrada e retorno da tela Newsletters continuam funcionando.
- [ ] Adicionar fontes RSS 2.0 e Atom por URL de feed ou URL de site continua funcionando.
- [ ] Descoberta de feeds, parser e limites de rede continuam intactos.
- [ ] Atualizar fonte selecionada e atualizar todas as fontes continuam disponíveis.
- [ ] Estado de carregamento, última atualização e erros por fonte continuam visíveis.
- [ ] Seleção de fonte e lista de artigos continuam funcionando.
- [ ] Marcar como lido/não lido continua persistindo.
- [ ] Leitor interno continua exibindo título e conteúdo/resumo disponível.
- [ ] Abrir original continua abrindo a URL do artigo.
- [ ] Salvar na Biblioteca continua criando referência em Ler depois e vinculando o artigo ao ID salvo.
- [ ] Excluir fonte continua preservando referências já salvas na Biblioteca.
- [ ] Não introduzir scraping autenticado, bypass de paywall ou sincronização cloud.

## 8. Settings e preferências

- [ ] Tema system/light/dark continua disponível e persistido em SharedPreferences.
- [ ] Seguir sistema continua acompanhando a configuração do dispositivo.
- [ ] Ordenação de categorias continua persistida.
- [ ] Perfil de compressão continua persistido e aplicado aos fluxos de imagem.
- [ ] Não alterar nomes de chaves, valores persistidos ou defaults sem aprovação explícita.

## 9. Revisão visual por feature

- [ ] Biblioteca: card de categoria, previews e ação Ver todas em tema claro/escuro.
- [ ] Dashboard: painel Meu progresso e fichas por categoria em tema claro/escuro.
- [ ] Pendentes/concluídas: badges e filtros no estado selecionado/não selecionado.
- [ ] Notes: campo pautado, imagem, recorte, erro, loading e salvamento.
- [ ] Fullscreen: header/título, mídia, player, notas, dock de ações e sub-checklist.
- [ ] Newsletters: fontes, artigos, status, estados vazios e leitor interno.
- [ ] Captura: painel de importação, câmera, revisão e recorte.
- [ ] Settings/menu: tema system/light/dark e legibilidade dos menus.
- [ ] Acessibilidade: tela estreita, fonte ampliada, títulos longos e contraste em ambos os temas.

## 10. Integridade técnica e privacidade

- [ ] MVVM e limites Feature-first / Presentation / Domain / Data permanecem.
- [ ] Nenhum Composable passa a acessar DAO, Room ou rede diretamente.
- [ ] Nenhuma regra de negócio migra para o tema ou componente visual.
- [ ] Nenhum modelo, contrato, UseCase, ViewModel, migration, schema ou formato serializado é alterado pelo rebrand.
- [ ] Versão Room e migrations permanecem inalteradas.
- [ ] Nenhum dado ou imagem capturada é enviado para cloud.
- [ ] Nenhuma permissão nova é introduzida.
- [ ] Executar testes unitários e `assembleDebug`; registrar CI do commit candidato.
- [ ] Comparar screenshots Light/Dark e testar tela estreita, fonte ampliada, títulos longos, ausência de imagem, estados vazios/erro/loading e modais.


## 11. Melhorias aprovadas: criação manual, capa padrão, exclusão e movimento

- [ ] A opção Nova tarefa aparece ao lado de Fotografar e Galeria.
- [ ] Tarefa manual exige título e categoria válida, aceita até 160 caracteres e permite títulos repetidos.
- [ ] Tarefa manual persiste como texto, sem imagem, URL de origem ou processamento OCR.
- [ ] Tarefa sem imagem exibe folha pautada padrão em miniatura, preview de categoria e Fullscreen.
- [ ] Capa padrão é desenhada em Compose; nenhum arquivo placeholder é criado/persistido.
- [ ] Na aba Concluídas, o card oferece excluir além de reabrir/alternar estado.
- [ ] Excluir tarefa concluída solicita confirmação e reutiliza limpeza de mídia, notas e subtarefas do fluxo existente.
- [ ] Entrada/saída de cards, status e progresso animam suavemente sem atrasar a ação.
- [ ] Imagens entram com fade-in sem decodificação na main thread.
- [ ] Animações não bloqueiam navegação, Back, clique ou acessibilidade quando a escala do sistema está reduzida.
