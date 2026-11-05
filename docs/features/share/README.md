# Feature Share

**Atualizado:** 2026-11-05  
**Camada:** Feature-first / MVVM  
**Ownership:** payload de domínio em Share; os dados da referência continuam pertencendo a Captures.

## Objetivo

Compartilhar uma referência completa como documento portátil, preservando texto, imagens e ordem. A feature oferece copiar texto, compartilhar via Android Sharesheet, exportar PDF e exportar RTF.

## Fluxo atual

1. Fullscreen solicita um snapshot da referência ativa, incluindo título, categoria, data, URL, conteúdo, capa, mídias ordenadas e blocos de Notes.
2. **Compartilhar PDF** gera um único PDF em cacheDir/share_exports/.
3. O PDF é exposto à Sharesheet por FileProvider com URI content://, MIME application/pdf e permissão temporária somente de leitura. O arquivo não é enviado como imagem nem como file:// diretamente.
4. O arquivo temporário é mantido no cache para que o aplicativo de destino possa consumi-lo; arquivos com mais de 24 horas são removidos na próxima preparação de compartilhamento.
5. **PDF** e **RTF** continuam disponíveis como exportação pelo Storage Access Framework (SAF), com destino escolhido pelo usuário.
6. **Copiar texto** envia somente conteúdo textual, sem imagens.

## Ordem de conteúdo do PDF

A ordem é determinística e deve ser preservada:

1. Título e categoria.
2. Informações: data de criação, link de origem (quando existir) e conteúdo textual.
3. Imagem de capa.
4. Galeria, na ordem recebida pelo payload, removendo repetição da capa e duplicatas.
5. Notes, na ordem persistida; cada bloco inclui seu texto e, em seguida, sua imagem opcional.

Imagem indisponível ou ilegível não deve ser omitida silenciosamente: a exportação falha com uma mensagem que identifica a imagem.

## Padrão visual obrigatório de PDF

Toda exportação PDF da feature deve seguir o padrão abaixo. Alterações futuras no layout devem atualizar esta seção e os testes de exportação.

### Página e margens
- Formato A4 retrato: 595 × 842 pontos (aproximação Android PdfDocument em 72 dpi).
- Fundo branco; cabeçalho em faixa papel #F3EBDD.
- Margens laterais de 48 pt; área de conteúdo de 499 pt.
- Rodapé a 36 pt da borda inferior, com linha divisória, CAPTURE FLOW à esquerda e número da página à direita.
- Conteúdo longo flui para páginas seguintes; nunca desenhar sobre o rodapé.

### Paleta
| Uso | Cor |
|---|---|
| Texto principal | #39382F |
| Texto secundário | #756F61 |
| Destaque/títulos de seção | #9A7138 |
| Fundo de cabeçalho | #F3EBDD |
| Linhas/divisores | #D8CCB8 |
| Fundo da página | #FFFFFF |

### Tipografia e hierarquia
| Elemento | Estilo |
|---|---|
| Título da referência | Sans-serif bold, 24 pt |
| Categoria | Sans-serif medium, 10 pt, cor de destaque |
| Cabeçalho/branding | Sans-serif bold, 9 pt |
| Seções | Sans-serif bold, 9 pt, caixa alta, cor de destaque |
| Texto e Notes | Sans-serif regular, 11 pt |
| Metadados e legendas de imagem | Sans-serif regular/medium, 9 pt |
| Rodapé | Sans-serif regular, 8 pt |

### Imagens
- Preservar proporção; nunca esticar.
- Ajustar dentro de 499 × 300 pt, sem ampliar imagens menores.
- Alinhar à margem esquerda do conteúdo.
- Legenda imediatamente antes da imagem, em 9 pt medium.
- Decodificar em background e limitar imagem decodificada a 1400 px por dimensão.
- Manter capa, galeria e imagens de Notes na ordem especificada.
- Se uma imagem esperada não puder ser lida/decodificada, abortar a exportação e informar erro.

### Paginação e conteúdo
- Quebrar parágrafos pela largura disponível, preservar quebras de linha e quebrar também palavras isoladas maiores que a largura.
- Iniciar nova página antes de blocos/imagens que não caibam; repetir cabeçalho e manter rodapé numerado.
- Não adicionar elementos decorativos que prejudiquem leitura, nem incluir informação que não esteja no payload.
- PDF compartilhado e PDF exportado via SAF usam o mesmo CaptureShareExporter e o mesmo padrão.

## Arquitetura

- domain/model/CaptureSharePayload: snapshot imutável sem tipos Android.
- domain/repository/CaptureShareRepository: contrato de exportação PDF/RTF.
- domain/usecase/CaptureShareUseCases: coordenação dos exportadores.
- data/CaptureShareExporter: geração PDF/RTF e leitura de imagens.
- presentation/viewmodel/CaptureShareViewModel: estado de exportação e erros.
- presentation/screens/CaptureShareDialog: clipboard, criação SAF e Android Sharesheet.
- FileProvider permanece não exportado, com grants temporários read-only e paths restritos a files/captures e cache/share_exports.

## Testes e aceite

- testDebugUnitTest
- assembleDebug
- assembleDebugAndroidTest
- Verificar magic header %PDF, abertura em leitores comuns, múltiplas páginas, capa, ordem da galeria e texto/imagem de Notes.
- Testar Android Sharesheet e recebimento do PDF em Gmail, WhatsApp e Drive; verificar extensão .pdf, MIME application/pdf e acesso após escolher o destino.
- Confirmar que o fluxo Compartilhar não envia anexos image/* nem ACTION_SEND_MULTIPLE.
