# Categories

CategoriesViewModel expõe criação, renomeação, exclusão e seleção. ManageCategoriesUseCase valida nomes, protege categorias padrão e reatribui referências ao excluir categoria personalizada. Persistência via contrato público de Captures; sem DAO/Room na feature.

Filtros em duas linhas com rolagem horizontal compartilhada; Ver todas abre modal. Padrão: Ler depois, Verificar, Salvar, Importante. Todas é filtro. Conclusão não é categoria; não recriar Concluído.