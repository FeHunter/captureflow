# Features

O código de produto é organizado por ownership em `features/{feature-name}`. Cada feature contém Presentation, Domain e Data quando aplicável. As dependências entre features devem ocorrer por contratos públicos/use cases, nunca por entidades Room ou DAOs.

Features ativas/estruturadas:
- `captures`: ciclo de vida de referências, categorias de domínio e subtarefas.
- `imageimport`: compartilhamento de conteúdo, metadados, importação de imagens e OCR.
- `categories`: gerenciamento e seleção de categorias.
- `search`: consulta e filtragem da biblioteca.
- `dashboard`: resumo reativo do progresso.
- `settings`: preferências locais do aplicativo.
- `fullscreen`: visualização ampliada de referências.

A tabela histórica `checklist_tasks` permanece no schema Room como compatibilidade de dados; não há feature/tela independente de Checklists. As subtarefas ativas pertencem a `captures` e usam `capture_subtasks`.

Consulte o README de cada feature antes de alterar responsabilidades.
