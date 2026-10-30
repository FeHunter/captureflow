# Features — organização por funcionalidade

O código de produto deve ser organizado em `features/{feature-name}`. Cada feature é dona de suas regras, estado e apresentação; adotar `data/`, `domain/` e `presentation/` quando aplicável, sem criar camadas vazias.

Features planejadas: `captures`, `imageimport`, `search`, `categories`, `fullscreen`, `checklists`, `dashboard` e `settings`.

A estrutura é incremental: consulte `AGENTS.md` e cada README de feature antes de implementar. Features ainda não extraídas do fluxo legado não devem ser tratadas como concluídas.
