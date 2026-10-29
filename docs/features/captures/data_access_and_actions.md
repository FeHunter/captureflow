# Acesso a dados e ações — Captures

## Persistência local
- `CaptureDao` persiste referências e observa alterações ordenadas por data.
- `ChecklistDao` persiste tarefas e observa alterações ordenadas por data.
- O ViewModel chama DAOs em coroutines; a View consome os fluxos expostos pelo ViewModel.

## Captura de imagens
- `SharedImageStorageService` tenta copiar imagens para `filesDir/captures`.
- Compartilhamento de imagem recebido pelo Android cria uma referência na aba Referências; o seletor da aba Checklist cria uma tarefa.
- Se a cópia falhar, a URI original é mantida como fallback; o acesso futuro pode depender da permissão concedida pelo provedor.
- Ao excluir uma captura/tarefa, o app tenta apagar apenas arquivos locais dentro do diretório privado de captures.

## Links
- `LinkMetadataService` tenta ler `og:title`, `og:description`, Twitter Card e tags HTML padrão.
- Requisições têm timeout e limite de bytes lidos.
- Redirecionamentos automáticos estão desativados; páginas que redirecionam podem não fornecer metadados.
- A URL original continua sendo a fonte de recuperação caso os metadados não estejam disponíveis.

## OCR
- `OcrService` usa ML Kit local e retorna texto completo mais uma sugestão de título e descrição.
- A heurística usa caixas delimitadoras e posição visual. Não há garantia de detecção real de bold ou entendimento semântico.
