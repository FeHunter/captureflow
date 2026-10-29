# Testes e critérios de aceite — Captures

**Status:** checklist manual; build/validação em dispositivo deve ser confirmado após esta alteração.

## Versão e cabeçalho
- [ ] Cabeçalho exibe a versão configurada no Gradle.
- [ ] Alterar `versionName` atualiza o texto sem modificar o composable.
- [ ] A versão de release e a versão de debug correspondem ao BuildConfig gerado.

## Imagem e cartão de referência
- [ ] Selecionar screenshot na aba Referências salva a URI, mostra a imagem como capa e inicia OCR automaticamente.
- [ ] Compartilhar screenshot de outro app cria referência com capa na aba Referências e inicia OCR automaticamente.
- [ ] Selecionar screenshot pelo botão da aba Checklist cria tarefa com capa.
- [ ] A capa persiste após encerrar e reabrir o app.
- [ ] Tocar na capa abre fullscreen, mantém proporção da imagem e permite fechar.
- [ ] Adicionar sub-item, marcar/desmarcar e excluir sub-item; confirmar persistência após reiniciar.
- [ ] Excluir uma referência também remove seus sub-itens.
- [ ] Imagem indisponível não quebra o cartão.
- [ ] Excluir referência tenta remover a cópia privada.

## OCR
- [ ] Testar screenshot da BBC: o título deve combinar as três linhas da manchete: “Luxury cruise passengers stranded in Cambodia due to soaring fuel costs”.
- [ ] Confirmar que a terceira linha, mesmo sendo mais estreita que as duas primeiras, não é descartada.
- [ ] Confirmar que horário, URL, autoria e ações do navegador não entram no título.
- [ ] Testar screenshot da Visual Capitalist: título esperado começa por “Mapped: Unemployment Rates Around the World in 2026”.
- [ ] Testar screenshot do YouTube mobile: título esperado começa por “10 Easy craft ideas / miniature craft / Paper”; não usar `m.youtube.com/watch` ou `Open App` como título.
- [ ] Testar screenshots de famílias vídeo/social, notícia, comércio e produtividade; verificar que o perfil ajusta a região candidata sem forçar uma posição única.
- [ ] Confirmar que uma palavra como “share” ou “views” dentro de um título real não é removida quando não estiver em contexto de interface.
- [ ] Confirmar que texto de likes/views/comments e navegação do player é filtrado do título.
- [ ] Título da referência contém a manchete completa sem horários, URL, autoria ou ações do navegador.
- [ ] O cartão mostra somente título, capa, checkbox e excluir; descrição/categoria/texto OCR bruto não aparecem.
- [ ] Enquanto OCR roda, o cartão mostra indicador de carregamento; em falha, mostra mensagem discreta.
- [ ] Imagens com layout diferente e imagens sem texto não causam crash.

## Links
- [ ] Compartilhar URL com metadados disponíveis e confirmar título/descrição.
- [ ] Testar URL que redireciona, exige login ou bloqueia acesso: referência original continua salva.
- [ ] Sem rede, a referência não é perdida.

## Banco e regressão
- [ ] Migrar banco v1 → v2 → v3 → v4 sem perder referências/tarefas.
- [ ] Criar, concluir e excluir tarefas sem imagem.
- [ ] Testar build debug e testes unitários.
