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
5. O usuário pode executar OCR local. A análise procura identificar título e texto complementar, mantendo a imagem original.
6. Na aba Checklist, uma imagem pode ser salva como tarefa com descrição opcional.

## Regras
- O conteúdo persistido não é enviado para um backend ou serviço de analytics.
- Recuperar metadados de páginas exige conexão de rede; se falhar, a URL original permanece salva.
- OCR roda localmente com ML Kit.
- A imagem original permanece vinculada à referência/tarefa, mesmo após o OCR.
- OCR é heurístico; não garante identificar semanticamente o título em todos os layouts.
- A versão exibida no cabeçalho vem de `BuildConfig.VERSION_NAME`, não de texto duplicado na interface.

## Limites conhecidos
- Sites podem bloquear requisições HTTP, exigir autenticação, depender de JavaScript ou omitir metadados.
- O reconhecimento de texto não fornece uma garantia universal de peso tipográfico (bold). A heurística usa caixas delimitadoras, altura/largura relativas e posição.
- A descoberta automática de screenshots em segundo plano não está implementada; o usuário seleciona ou compartilha a imagem explicitamente.
