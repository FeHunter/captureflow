# Roadmap

Este roadmap separa recursos implementados no código de validação pendente. A existência de código não significa que todos os fluxos foram testados em dispositivos reais.

## Base nativa
- [x] App Android nativo Kotlin + Jetpack Compose.
- [x] MVVM com ViewModel, Coroutines e Flow/StateFlow.
- [x] Persistência Room/SQLite para referências, tarefas legadas e sub-checklists.
- [x] Entrada de texto/URL/imagem via Android Sharesheet.
- [x] Importação de imagem via Photo Picker.
- [x] Busca e filtros por categoria na tela Referências.
- [x] Concluir/excluir referências e alterar categoria.
- [x] OCR local ML Kit com sugestão heurística de título.
- [x] Visualização fullscreen e navegação por swipe entre referências.
- [x] Sub-checklist persistente por referência.
- [x] Tema sistema/claro/escuro.
- [x] Monitoramento opcional de screenshots com notificação persistente.
- [ ] CI/build aprovado para o commit atual (acompanhar PR #25 e Actions).
- [ ] Testes em emulador e dispositivo físico dos fluxos críticos.

## Correções e qualidade prioritárias
- [ ] Alinhar o texto da notificação e do diálogo ao comportamento final de salvar screenshot como referência.
- [ ] Alinhar a categoria `Referência` com chips/filtros/menu de categorias.
- [ ] Validar intents recebidos em `onCreate` e `onNewIntent`, incluindo URI pendente, cancelamento e repetição.
- [ ] Testar acesso à URI de imagem com provedores diferentes e fallback de cópia privada.
- [ ] Validar monitoramento, permissões, notificação, stop action e restrições de bateria em diferentes fabricantes.
- [ ] Corrigir o texto secundário do cartão caso a intenção seja mostrar informação diferente do título; atualmente o código renderiza `capture.title` nas duas posições.
- [ ] Melhorar feedback explícito para falhas de importação/persistência.
- [ ] Expandir testes de normalização/deduplicação, OCR heurístico e regras de sub-checklist.
- [ ] Verificar acessibilidade, escala de fonte e layout em telas estreitas.

## Publicação
- [ ] Revisar políticas atuais do Google Play para foreground service `specialUse` e declaração exigida.
- [ ] Fazer revisão de permissões e texto de privacidade.
- [ ] Concluir validação funcional e de dispositivo antes de marcar release.

## Futuro — não implementado
- [ ] Backup/sincronização opcional com consentimento explícito.
- [ ] Integrações externas de tarefas ou IA em nuvem após análise de privacidade.
- [ ] Melhorias de classificação automática após avaliar dados e testes.

## Gate de conclusão
Uma entrega só deve ser considerada pronta após revisão do diff, testes pertinentes, build Gradle do commit exato e validação de dispositivo adequada.
