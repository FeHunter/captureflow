# Detecção de título por OCR

## Objetivo

Selecionar um título provável em screenshots e imagens importadas sem alterar o texto integral reconhecido pelo ML Kit nem depender de regras exclusivas para cada aplicativo.

## Fluxo implementado

1. `OcrService` cria a `InputImage` e executa o ML Kit localmente.
2. Cada linha reconhecida é convertida para `OcrTextLine` com texto, caixa delimitadora e confiança.
3. `TitleDetectionService` (Domain puro, sem Android) filtra ruído, classifica a família visual e pontua os candidatos.
4. Linhas adjacentes e visualmente compatíveis são agrupadas para formar títulos multilinha.
5. As linhas não selecionadas formam a descrição. O texto integral do OCR permanece em `OcrTextResult.fullText`.

## Sinais da pontuação

- **Altura relativa:** compara a altura da caixa de cada linha com a mediana das linhas reconhecidas.
- **Área relativa:** compara `largura × altura` da caixa com a área mediana das demais linhas.
- **Largura relativa:** contribui com peso menor, para evitar que URLs ou controles horizontais ganhem apenas por serem largos.
- **Posição vertical:** favorece a região central e aplica ajustes leves conforme o perfil aproximado (vídeo, social, notícia, compras, produtividade ou genérico).
- **Confiança do OCR:** contribui quando disponível. Valor `0` é tratado como indisponível/neutro, pois algumas versões do ML Kit não retornam confiança.
- **Forma textual:** comprimento, proporção de palavras e sinais de texto editorial.
- **Ruído de interface:** URLs, domínios, horários isolados, `Open App`, handles, contagens de engajamento e rótulos de navegação isolados são descartados.

A largura isolada tem influência deliberadamente pequena. A posição central é um sinal, não uma restrição rígida: manchetes próximas do topo continuam elegíveis quando os demais sinais visuais são fortes.

## Negrito e destaque tipográfico

O ML Kit fornece caixas, elementos, símbolos, ângulo, idioma e confiança, mas não um atributo confiável de peso tipográfico (bold). A implementação atual **não afirma detectar negrito**. Altura e área da caixa são proxies de proeminência visual, não prova de espessura dos traços.

Uma análise futura de pixels pode estimar densidade/espessura dos traços, mas deve ser validada em fundos claros/escuros, fontes finas, sombras, texto colorido e imagens comprimidas antes de entrar na pontuação. OpenCV não foi adicionado nesta entrega.

## Agrupamento de títulos multilinha

A partir da linha âncora mais bem pontuada, o detector expande para linhas acima e abaixo quando:
- a distância vertical é pequena;
- a altura é compatível com a linha âncora;
- as linhas compartilham alinhamento/área horizontal;
- o texto não parece ruído de interface.

Isso evita juntar automaticamente parágrafos próximos e permite linhas de manchete ligeiramente menores.

## Testes unitários

`TitleDetectionServiceTest` cobre:
- preferência por manchete visualmente proeminente em relação a navegação e parágrafos;
- agrupamento de manchete em duas linhas;
- filtragem de URL, `Open App` e métricas de engajamento;
- preservação de palavras como “share” quando fazem parte de uma manchete;
- OCR vazio;
- confiança indisponível.

## Limitações e validação

- Heurísticas são independentes da UI e não reconhecem oficialmente o aplicativo de origem.
- Ainda é necessário validar em dispositivo com screenshots reais e variados (idioma, tema, versão, anúncios e densidade de texto).
- Não há dependência de OpenCV nem uma segunda passagem de OCR nesta etapa.
- O texto integral continua disponível; título e descrição são sugestões editáveis no fluxo de revisão da importação.

## Referências técnicas

- [Google ML Kit — Text Recognition v2 para Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android): hierarquia de blocos/linhas/elementos/símbolos e diretrizes de qualidade da imagem.
- [Google ML Kit — Text.Line](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line): caixa delimitadora, confiança, ângulo, pontos e idioma reconhecido.
