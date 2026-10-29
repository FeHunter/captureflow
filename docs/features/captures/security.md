# Segurança e privacidade — Captures

- Referências, descrições e imagens são persistidas localmente no dispositivo.
- Imagens são copiadas para o diretório privado do app quando possível.
- Para seleção manual de imagem, usar o seletor de sistema sem permissão ampla. A detecção opcional de screenshots exige permissão de leitura de imagens e explica seu propósito antes do pedido.
- Metadados de links exigem uma requisição de rede para a URL escolhida pelo usuário. Não enviar o conteúdo capturado a um serviço de IA externo.
- A requisição de metadados limita o tempo de conexão/leitura e o volume de HTML, aceita apenas HTTP(S) com host em formato de domínio público e não segue redirecionamentos automaticamente.
- A remoção de imagem deve restringir-se ao diretório privado do app; nunca apagar arquivos de URI externos.
- Revisar proteção contra SSRF/hosts especiais e redirects caso o serviço de metadados seja alterado.

- A detecção de screenshots é opcional e só observa o MediaStore enquanto o app está em primeiro plano; não usa Accessibility Service, não monitora conteúdo de outros apps e não mantém serviço em segundo plano.
- A detecção é heurística e depende de nomes de arquivo/pastas expostos pelo MediaStore. Capturas com nomes/pastas personalizados ou em dispositivos com restrições de acesso podem não ser reconhecidas.


## Monitoramento de screenshots em segundo plano
- Ativação explícita do usuário; permissão de leitura de imagens e permissão de notificações (Android 13+) são pré-requisitos.
- O serviço em primeiro plano usa tipo `specialUse`, com subtipo declarado no Manifest. Antes de distribuir pela Google Play, validar as políticas atuais de foreground services e a declaração/revisão exigida para `specialUse`.
- O monitor consulta apenas metadados recentes do MediaStore e não usa Accessibility Service nem captura o conteúdo de tela de outros apps.
- As notificações expõem ações rápidas para salvar uma referência ou criar uma tarefa; as imagens são salvas no armazenamento privado ao serem importadas pelo fluxo existente.
- Restrições de bateria, force-stop, permissões negadas e nomes/pastas não padronizados podem impedir ou atrasar a detecção.
