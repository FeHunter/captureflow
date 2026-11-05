# Backup

ZIP v1 via Storage Access Framework: manifest.json versionado e images/{sha256}.bin. Exporta referências, categorias, conclusão, subtarefas, mídias, notas e imagens; SHA-256 deduplica imagens. URIs de origem não são reutilizadas; imagens restauradas em storage privado.

Importação aditiva: nunca apaga/sobrescreve. Categoria existente preserva ordenação; ausentes são adicionadas. Duplicatas são ignoradas pela identidade implementada (URL normalizada quando houver + título, conteúdo, tipo, categoria, conclusão e resumo de notas). Colisões de ID geram novos IDs e remapeiam filhos. Inserção Room transacional; falha limpa arquivos temporários.

Limites: manifesto 10 MB, imagem 25 MB, descompactado 250 MB, 50.000 registros/50.002 entradas. v1 não inclui Newsletters nem preferências. Tela no menu Backup e restauração; CI e testes em dispositivo com arquivos grandes/conflitos ainda precisam ser verificados.