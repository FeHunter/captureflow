# Notes

Feature responsável pela experiência de edição das anotações associadas a uma referência.

## Responsabilidades
- Apresentar o editor amplo de anotações em modal.
- Limitar o conteúdo a 10.000 caracteres e exibir contador.
- Oferecer ações explícitas para salvar ou cancelar.

## Ownership e persistência
- O texto pertence ao agregado `Capture` e permanece persistido pela feature `captures`.
- Esta feature não acessa Room, DAOs ou repositórios de Captures.
- O app coordinator conecta a ação de abertura do Fullscreen ao editor e encaminha o texto confirmado ao `CaptureViewModel`.
- Estado temporário do campo permanece local ao Composable; não existe estado de domínio independente que justifique outro ViewModel.

## Ponto de entrada
- `presentation/components/NotesEditorDialog.kt`: modal de edição.
