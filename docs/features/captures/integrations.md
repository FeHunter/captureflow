# Integrações — Captures

## ML Kit Text Recognition
Usado localmente para reconhecer texto em imagens. Não há upload da imagem para análise. A saída estruturada é uma heurística construída a partir das linhas e caixas delimitadoras fornecidas pelo ML Kit.

## Android Sharesheet e Photo Picker
- Recebe compartilhamentos explícitos por intents suportadas no Manifest.
- Usa Photo Picker do Android para seleção manual sem pedir acesso amplo à galeria.
- O fluxo de detecção automática de screenshots em segundo plano não está implementado.

## HTTP(S) e metadados de páginas
- Requisição direta para o host da URL salva.
- Busca título/descrição em metadados HTML comuns.
- Não executa JavaScript, não faz scraping visual e não segue redirecionamentos automaticamente.
- Uma página que bloqueie acesso pode não fornecer metadados; o app mantém a referência original.

## Dependências locais
Room/SQLite, Coroutines/Flow e Jetpack Compose. Não existe sincronização cloud implementada para a feature.
