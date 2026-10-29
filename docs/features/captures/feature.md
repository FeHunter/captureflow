# Contrato funcional — Captures

**Versão do app:** 0.3.2  
**Status:** implementação em validação no emulador.

## Objetivo
Permitir que o usuário guarde conteúdo encontrado durante o uso do dispositivo como referência ou tarefa, mantendo os dados no aparelho.

## Fluxos funcionais
1. O usuário compartilha texto/URL com CaptureFlow; a entrada manual de texto/link não aparece na tela principal.
2. O app salva uma referência local e, para URLs HTTP(S), tenta recuperar título e descrição do HTML público.
3. O usuário seleciona uma imagem na tela principal de Referências ou compartilha uma imagem/screenshot com o app; ao selecionar manualmente, escolhe uma categoria/tag antes de salvar. A imagem é copiada para o armazenamento privado quando possível.
4. A tela principal reúne a seleção de imagem e a biblioteca de referências, sem bottom navigation. A checklist independente não é exibida nesta tela.
5. Assim que uma imagem é selecionada ou compartilhada para a aba Referências, o app inicia OCR local automaticamente e atualiza o título da referência; o cartão mantém a imagem original como capa.
6. O cartão de referência exibe apenas título, capa, checkbox de conclusão e botão de exclusão. Durante a análise, pode exibir um indicador de carregamento ou uma mensagem de erro.
7. Ao tocar na capa de uma referência, a imagem abre em visualização fullscreen com opção de fechar.
8. Cada referência pode ter uma sub-checklist persistida, com itens adicionáveis, concluíveis e removíveis.
9. Uma imagem pode ser salva como tarefa com descrição opcional; esse fluxo continua separado do cartão de Referências, mas acessível na mesma tela.

## Regras
- O conteúdo persistido não é enviado para um backend ou serviço de analytics.
- Recuperar metadados de páginas exige conexão de rede; se falhar, a URL original permanece salva.
- OCR roda localmente com ML Kit.
- A imagem original permanece vinculada à referência/tarefa, mesmo após o OCR.
- Para referências baseadas em imagem, o resultado persistido da análise é o título; o texto complementar do OCR não é mostrado nem armazenado no campo de conteúdo da referência.
- A análise de imagem é iniciada automaticamente após salvar a cópia local; não há botão manual de OCR no cartão de referência.
- OCR usa perfis heurísticos de famílias visuais (vídeo, social, notícias, comércio e produtividade) para ajustar a pontuação do título e filtrar ruído conhecido.
- OCR é heurístico; não garante identificar semanticamente o título em todos os layouts.
- A versão exibida no cabeçalho vem de `BuildConfig.VERSION_NAME`, não de texto duplicado na interface.
- A sub-checklist de uma referência é armazenada em uma tabela Room separada e é removida quando a referência é excluída.
- A heurística de OCR filtra linhas de interface do YouTube/navegador (URL `youtube.com`, `Open App`, likes/views/comments e navegação) antes de escolher o título; isso reduz ruído, mas não garante sucesso em todos os layouts.

## Limites conhecidos
- Sites podem bloquear requisições HTTP, exigir autenticação, depender de JavaScript ou omitir metadados.
- O reconhecimento de texto não fornece uma garantia universal de peso tipográfico (bold). A heurística usa caixas delimitadoras, altura/largura relativas e posição.
- A descoberta automática de screenshots em segundo plano não está implementada; o usuário seleciona ou compartilha a imagem explicitamente.

## Tema e organização visual
- A paleta inspirada na imagem de referência usa azul-marinho, azul oceano, azul-ciano e superfícies claras no tema claro.
- O usuário pode selecionar Seguir dispositivo, Claro ou Escuro; a preferência é persistida localmente.
- A seleção manual de imagem permite atribuir uma das categorias existentes antes de criar a referência.
- Referências e tarefas independentes são apresentadas em uma única tela; não há bottom navigation.


## Tela principal simplificada
- A tela mantém a seleção de imagem, a busca e os filtros por categoria.
- A seção independente com o título Checklist e o formulário manual de texto/link não são exibidos na tela principal.
- O campo de busca mostra um botão de limpar (X) somente enquanto há texto digitado.
- O título do app usa a versão base mais o identificador curto do commit, gerado pelo Gradle para diferenciar cada commit.


## Detecção de screenshots — v0.3.2
- O usuário pode conceder acesso às imagens para ativar a detecção best-effort de screenshots recentes.
- Com permissão concedida, o app observa o MediaStore apenas enquanto está em primeiro plano e identifica imagens recentes pelo nome do arquivo ou pasta Screenshots.
- Ao detectar uma captura, abre um diálogo para confirmar o envio ao CaptureFlow e escolher a categoria antes de salvar e executar OCR.
- O seletor de imagem do sistema e o compartilhamento via Android Sharesheet continuam disponíveis sem habilitar a detecção.
- O título do app exibe somente a versão semântica numérica (`v0.3.2`), sem hash do commit.
