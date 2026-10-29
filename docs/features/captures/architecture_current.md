# Arquitetura atual — Captures

## Stack
- Kotlin, Android SDK, Jetpack Compose.
- MVVM com `CaptureViewModel` e `StateFlow`.
- Room/SQLite para persistência local.
- Coroutines para trabalho assíncrono.
- ML Kit Text Recognition para OCR on-device.

## Fluxo
```text
CaptureFlowApp (Compose View)
        ↓ eventos / estado coletado
CaptureViewModel (orquestração e regras)
        ├── CaptureDao / ChecklistDao → Room / SQLite
        ├── OcrService → ML Kit local
        ├── LinkMetadataService → HTML público HTTP(S)
        └── SharedImageStorageService → arquivos privados do app
```

## Responsabilidades
- **View:** renderiza estado e encaminha ações; não consulta Room diretamente.
- **ViewModel:** coordena captura, análise OCR, enriquecimento de links e operações CRUD.
- **Services:** isolam IO de OCR, requisições HTTP e cópia/remoção de imagens.
- **DAO/Room:** persistem entidades e emitem listas reativas.

## Diretriz de manutenção
Manter a lógica fora dos composables sempre que for regra de negócio ou IO. Não introduzir acesso de rede ou banco na View. Novas operações devem ter tratamento explícito de falha e preservar dados já capturados.
