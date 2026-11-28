package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.CategoryRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Category
import com.mcpratapp.repository.CategoryRepository
import com.mcpratapp.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var categoryService: CategoryService

    @Test
    fun `createCategory gera prefixo automatico quando skuPrefix e nulo ou em branco`() {
        val savedId = UUID.randomUUID()
        `when`(categoryRepository.findByNameIgnoreCase("Anéis")).thenReturn(null)
        `when`(categoryRepository.save(anyNonNull<Category>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Category>(0)
            Category(id = savedId, name = arg.name, skuPrefix = arg.skuPrefix, isActive = arg.isActive)
        }

        val response = categoryService.createCategory(CategoryRequest(name = "  Anéis  ", skuPrefix = "   "))

        assertEquals(savedId, response.id)
        assertEquals("Anéis", response.name)
        assertEquals("ANÉ", response.skuPrefix)
    }

    @Test
    fun `createCategory usa prefixo informado normalizado em maiusculas`() {
        val savedId = UUID.randomUUID()
        `when`(categoryRepository.findByNameIgnoreCase("Pulseiras")).thenReturn(null)
        `when`(categoryRepository.save(anyNonNull<Category>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Category>(0)
            Category(id = savedId, name = arg.name, skuPrefix = arg.skuPrefix, isActive = arg.isActive)
        }

        val response = categoryService.createCategory(CategoryRequest(name = "Pulseiras", skuPrefix = " pls "))

        assertEquals("PLS", response.skuPrefix)
    }

    @Test
    fun `createCategory lanca ConflictException para nome duplicado`() {
        val existing = Category(id = UUID.randomUUID(), name = "Colares", skuPrefix = "COL")
        `when`(categoryRepository.findByNameIgnoreCase("Colares")).thenReturn(existing)

        assertThrows(ConflictException::class.java) {
            categoryService.createCategory(CategoryRequest(name = "Colares"))
        }
    }

    @Test
    fun `getAllActiveCategories retorna categorias ordenadas`() {
        val c1 = Category(id = UUID.randomUUID(), name = "Anéis", skuPrefix = "ANE")
        val c2 = Category(id = UUID.randomUUID(), name = "Brincos", skuPrefix = "BRI")
        `when`(categoryRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(listOf(c1, c2))

        val result = categoryService.getAllActiveCategories()

        assertEquals(2, result.size)
        assertEquals("Anéis", result[0].name)
        assertEquals("Brincos", result[1].name)
    }

    @Test
    fun `updateCategory atualiza nome e mantem prefixo quando novo prefixo e vazio`() {
        val id = UUID.randomUUID()
        val existing = Category(id = id, name = "Anéis", skuPrefix = "ANE")
        `when`(categoryRepository.findById(id)).thenReturn(Optional.of(existing))
        `when`(categoryRepository.existsByNameIgnoreCaseAndIdNot("Anéis Prata", id)).thenReturn(false)
        `when`(categoryRepository.save(anyNonNull<Category>())).thenAnswer { it.getArgument<Category>(0) }

        val updated = categoryService.updateCategory(id, CategoryRequest(name = " Anéis Prata ", skuPrefix = " "))

        assertEquals("Anéis Prata", updated.name)
        assertEquals("ANE", updated.skuPrefix)
    }

    @Test
    fun `updateCategory lanca ResourceNotFoundException quando categoria nao existe`() {
        val id = UUID.randomUUID()
        `when`(categoryRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) {
            categoryService.updateCategory(id, CategoryRequest(name = "Nova"))
        }
    }

    @Test
    fun `updateCategory lanca ConflictException quando outra categoria tem mesmo nome`() {
        val id = UUID.randomUUID()
        val existing = Category(id = id, name = "Anéis", skuPrefix = "ANE")
        `when`(categoryRepository.findById(id)).thenReturn(Optional.of(existing))
        `when`(categoryRepository.existsByNameIgnoreCaseAndIdNot("Colares", id)).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            categoryService.updateCategory(id, CategoryRequest(name = "Colares"))
        }
    }

    @Test
    fun `deactivateCategory desativa categoria sem produtos ativos`() {
        val id = UUID.randomUUID()
        val existing = Category(id = id, name = "Anéis", skuPrefix = "ANE", isActive = true)
        `when`(categoryRepository.findById(id)).thenReturn(Optional.of(existing))
        `when`(productRepository.existsByCategoryIdAndIsActiveTrue(id)).thenReturn(false)
        `when`(categoryRepository.save(anyNonNull<Category>())).thenAnswer { it.getArgument<Category>(0) }

        val result = categoryService.deactivateCategory(id)

        assertFalse(result.isActive)
        verify(categoryRepository).save(existing)
    }

    @Test
    fun `deactivateCategory lanca ConflictException quando ha produtos ativos vinculados`() {
        val id = UUID.randomUUID()
        val existing = Category(id = id, name = "Anéis", skuPrefix = "ANE", isActive = true)
        `when`(categoryRepository.findById(id)).thenReturn(Optional.of(existing))
        `when`(productRepository.existsByCategoryIdAndIsActiveTrue(id)).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            categoryService.deactivateCategory(id)
        }
    }
}
