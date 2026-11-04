# YouTube Player

Feature responsável exclusivamente pela reprodução incorporada de vídeos do YouTube em Android nativo.

## Responsabilidades
- Construir a URL oficial de incorporação em `youtube-nocookie.com/embed/{VIDEO_ID}`.
- Enviar a identidade do app explicitamente no cabeçalho HTTP `Referer`, usando `WebView.loadUrl(url, additionalHttpHeaders)`.
- Definir também o parâmetro `origin` da incorporação, alinhado ao applicationId.
- Aplicar controles habilitados, reprodução inline e autoplay desabilitado.
- Configurar `WebChromeClient` junto ao `WebViewClient` para dar suporte à camada multimídia do Chromium e à renderização do vídeo incorporado.
- Manter aceleração de hardware, lifecycle pause/resume, estados de carregamento/erro e destruição segura da instância.
- Restringir navegação principal a hosts HTTPS do YouTube; sub-recursos do player continuam permitidos.

## Organização
- `presentation/YoutubeEmbedRequest.kt`: validação do ID e construção da URL/cabeçalho.
- `presentation/YoutubeEmbeddedPlayer.kt`: componente Compose, WebView e estado de apresentação.
- Não há Domain ou ViewModel porque a feature não possui regras de negócio nem estado persistente. O estado de carregamento/erro pertence à apresentação.

## Contrato de uso
O fullscreen fornece um videoId validado pelo parser compartilhado e posiciona YoutubeEmbeddedPlayer fora do HorizontalPager. O componente preserva proporção 16:9, exige pelo menos 200 dp de altura e não inicia reprodução automaticamente.

## Identidade e segurança
- Não falsificar User-Agent.
- Não expor APIs nativas ao JavaScript do player.
- Não habilitar acesso a arquivos locais nem abrir janelas múltiplas.
- O identificador do vídeo é validado contra `[A-Za-z0-9_-]{11}` antes de compor a URL.
- O app não tenta contornar restrições de incorporação impostas pelo proprietário do vídeo.

## Diagnóstico
- O `Referer` é enviado explicitamente no carregamento do endpoint de incorporação, seguindo a orientação do YouTube para WebViews sem HTML local.
- Erros de rede/HTTP na navegação principal são apresentados no componente, com opção de tentar novamente.
- Vídeos privados, removidos ou que bloqueiam incorporação continuam sujeitos às restrições do YouTube; nesses casos, o link original permanece disponível no cabeçalho do fullscreen.
