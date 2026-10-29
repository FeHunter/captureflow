# Contrato funcional — Captures

**Versão do app:** 0.3.0  
**Status:** implementação em validação no emulador.

## Objetivo
Permitir que o usuário guarde conteúdo encontrado durante o uso do dispositivo como referência ou tarefa, mantendo os dados no aparelho.

## Fluxos funcionais
1. O usuário cola texto/URL ou compartilha texto com CaptureFlow.
2. O app salva uma referência local e, para URLs HTTP(S), tenta recuperar título e descrição do HTML público.
3. O usuário seleciona uma imagem na aba Referências ou compartilha uma imagem/screenshot com o app; ela é salva como referência e copiada para o armazenamento privado quando possível.
4. Na aba Referências, a imagem anexada é exibida como capa do cartão. Na aba Checklist, o usuário pode selecionar a imagem separadamente para criar uma tarefa com screenshot.
5. Assim que uma imagem é selecionada ou compartilhada para a aba Referências, o app inicia OCR local automaticamente e atualiza o título da referência; o cartão mantém a imagem original como capa.
6. O cartão de referência exibe apenas título, capa, checkbox de conclusão e botão de exclusão. Durante a análise, pode exibir um indicador de carregamento ou uma mensagem de erro.
7. Ao tocar na capa de uma referência, a imagem abre em visualização fullscreen com opção de fechar.
8. Cada referência pode ter uma sub-checklist persistida, com itens adicionáveis, concluíveis e removíveis.
9. Na aba Checklist, uma imagem pode ser salva como tarefa com descrição opcional; esse fluxo é separado do cartão de Referências.

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
