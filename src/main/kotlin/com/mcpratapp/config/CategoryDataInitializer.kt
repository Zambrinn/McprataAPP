package com.mcpratapp.config

import com.mcpratapp.model.Category
import com.mcpratapp.repository.CategoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/*
    essa classe é so para popular as categorias base no primeiro build da imagem, pois a tabela vai estar vazia
    vou deixar a classe após ser populado para histórico de versionamento.
 */

@Component
class CategoryDataInitializer (
    private val categoryRepository: CategoryRepository
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (categoryRepository.count() == 0L) {
            val defaults = listOf(
                Category(name = "Anéis", skuPrefix = "ANL"),
                Category(name = "Pulseiras", skuPrefix = "PLS"),
                Category(name = "Colares", skuPrefix = "CLR"),
                Category(name = "Brincos", skuPrefix = "BRC"),
                Category(name = "Conjuntos", skuPrefix = "CNJ"),
                Category(name = "Outros", skuPrefix = "OUT")
            )
            categoryRepository.saveAll(defaults)
        }
    }
}
