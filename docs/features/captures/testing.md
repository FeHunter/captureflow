# Testes e critérios de aceite — Captures

**Status:** checklist manual; build/validação em dispositivo deve ser confirmado após esta alteração.

## Versão e cabeçalho
- [ ] Cabeçalho exibe a versão configurada no Gradle.
- [ ] Alterar `versionName` atualiza o texto sem modificar o composable.
- [ ] A versão de release e a versão de debug correspondem ao BuildConfig gerado.

## Imagem e cartão de referência
- [ ] Selecionar screenshot salva a URI e mostra a imagem como capa na aba Referências.
- [ ] A capa persiste após encerrar e reabrir o app.
- [ ] Imagem indisponível não quebra o cartão.
- [ ] Excluir referência tenta remover a cópia privada.

## OCR
- [ ] Testar screenshot da BBC: título esperado começa por “Luxury cruise passengers stranded in Cambodia due to soaring fuel costs”.
- [ ] Testar screenshot da Visual Capitalist: título esperado começa por “Mapped: Unemployment Rates Around the World in 2026”.
- [ ] Descrição mantém texto útil sem transformar horários/domínios em título.
- [ ] Texto original continua disponível na descrição após análise.
- [ ] Imagens com layout diferente e imagens sem texto não causam crash.

## Links
- [ ] Compartilhar URL com metadados disponíveis e confirmar título/descrição.
- [ ] Testar URL que redireciona, exige login ou bloqueia acesso: referência original continua salva.
- [ ] Sem rede, a referência não é perdida.

## Banco e regressão
- [ ] Migrar banco v1 → v2 → v3 sem perder referências/tarefas.
- [ ] Criar, concluir e excluir tarefas sem imagem.
- [ ] Testar build debug e testes unitários.
