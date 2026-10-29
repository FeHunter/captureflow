# Segurança e privacidade — Captures

- Referências, descrições e imagens são persistidas localmente no dispositivo.
- Imagens são copiadas para o diretório privado do app quando possível.
- Não solicitar permissão ampla de armazenamento apenas para seleção de imagens; usar o seletor de sistema.
- Metadados de links exigem uma requisição de rede para a URL escolhida pelo usuário. Não enviar o conteúdo capturado a um serviço de IA externo.
- A requisição de metadados limita o tempo de conexão/leitura e o volume de HTML, aceita apenas HTTP(S) com host em formato de domínio público e não segue redirecionamentos automaticamente.
- A remoção de imagem deve restringir-se ao diretório privado do app; nunca apagar arquivos de URI externos.
- Revisar proteção contra SSRF/hosts especiais e redirects caso o serviço de metadados seja alterado.
