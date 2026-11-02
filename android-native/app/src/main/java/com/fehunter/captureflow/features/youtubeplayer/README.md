# YouTube Player

Feature responsável exclusivamente pela reprodução incorporada de vídeos do YouTube em Android nativo.

## Responsabilidades
- Construir o documento HTML mínimo que hospeda a YouTube IFrame Player API.
- Incorporar o vídeo por meio de WebView dentro de AndroidView.
- Fornecer a identidade do cliente exigida pelo YouTube usando loadDataWithBaseURL("https://{applicationId}/", ...), com origin alinhado ao mesmo host.
- Aplicar strict-origin-when-cross-origin, controles habilitados, reprodução inline e autoplay desabilitado.
- Manter aceleração de hardware, lifecycle pause/resume, callbacks de pronto/erro/estado e destruição segura da instância.
- Restringir navegação principal a hosts HTTPS do YouTube; sub-recursos e iframe continuam permitidos.

## Organização
- data/YoutubePlayerHtml.kt: documento HTML e configuração IFrame.
- presentation/YoutubeEmbeddedPlayer.kt: componente Compose, WebView, estado de apresentação e lifecycle.
- Não há Domain ou ViewModel porque a feature não possui regras de negócio nem estado persistente. O estado de carregamento/erro pertence à apresentação.

## Contrato de uso
O fullscreen fornece um videoId validado pelo parser compartilhado e posiciona YoutubeEmbeddedPlayer fora do HorizontalPager. O componente preserva proporção 16:9, exige pelo menos 200 dp de altura e não inicia reprodução automaticamente.

## Identidade e segurança
- Não falsificar User-Agent.
- Não expor APIs nativas além de callbacks de prontidão, estado e erro do player.
- Não habilitar acesso a arquivos locais nem abrir janelas múltiplas.
- O HTML é gerado internamente e o identificador do vídeo é validado contra [A-Za-z0-9_-]{11} antes de interpolação.
- O app não tenta contornar restrições de incorporação impostas pelo proprietário do vídeo.

## Diagnóstico
- Erro 153: ausência de HTTP Referer ou identificação equivalente; conferir baseUrl e origin.
- Erros 101/150/152: o vídeo não permite incorporação.
- Erro 100: vídeo removido ou privado.
- Falhas de rede do documento principal exibem mensagem de conexão; erros reportados pela IFrame API exibem o código recebido.
- O link original da captura permanece acessível pelo cabeçalho do fullscreen.
