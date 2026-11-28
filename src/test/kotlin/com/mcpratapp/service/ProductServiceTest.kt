package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.ProductRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Category
import com.mcpratapp.model.Product
import com.mcpratapp.repository.CategoryRepository
import com.mcpratapp.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @InjectMocks
    private lateinit var productService: ProductService

    private fun activeCategory(id: UUID = UUID.randomUUID()) =
        Category(id = id, name = "Anéis", skuPrefix = "ANE", isActive = true)

    private fun sampleProduct(
        id: UUID = UUID.randomUUID(),
        category: Category = activeCategory(),
        totalQuantity: Int = 20,
        reservedQuantity: Int = 0,
        isActive: Boolean = true
    ) = Product(
        id = id,
        sku = "ANE-001",
        name = "Anel Prata 925",
        description = "Anel clássico",
        totalQuantity = totalQuantity,
        reservedQuantity = reservedQuantity,
        isActive = isActive,
        category = category
    )

    @Test
    fun `createProduct cria produto com categoria ativa e reservedQuantity zero`() {
        val category = activeCategory()
        val savedId = UUID.randomUUID()
        val request = ProductRequest(
            sku = "ANE-001",
            name = "Anel Prata 925",
            description = "Clássico",
            totalQuantity = 15,
            categoryId = category.id!!
        )

        `when`(productRepository.findBySku("ANE-001")).thenReturn(null)
        `when`(categoryRepository.findById(category.id!!)).thenReturn(Optional.of(category))
        `when`(productRepository.save(anyNonNull<Product>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Product>(0)
            Product(
                id = savedId,
                sku = arg.sku,
                name = arg.name,
                description = arg.description,
                totalQuantity = arg.totalQuantity,
                reservedQuantity = arg.reservedQuantity,
                isActive = arg.isActive,
                category = arg.category
            )
        }

        val response = productService.createProduct(request)

        assertEquals(savedId, response.id)
        assertEquals(0, response.reservedQuantity)
        assertEquals("ANE-001", response.sku)
        assertEquals(category.id, response.category?.id)
    }

    @Test
    fun `createProduct lanca ConflictException para SKU duplicado ou categoria inativa`() {
        val categoryId = UUID.randomUUID()
        val request = ProductRequest("ANE-001", "Anel", null, 10, categoryId)

        `when`(productRepository.findBySku("ANE-001")).thenReturn(sampleProduct())
        assertThrows(ConflictException::class.java) { productService.createProduct(request) }

        `when`(productRepository.findBySku("ANE-001")).thenReturn(null)
        val inactiveCategory = Category(id = categoryId, name = "Antiga", skuPrefix = "ANT", isActive = false)
        `when`(categoryRepository.findById(categoryId)).thenReturn(Optional.of(inactiveCategory))
        assertThrows(ConflictException::class.java) { productService.createProduct(request) }
    }

    @Test
    fun `createProduct lanca ResourceNotFoundException quando categoria nao existe`() {
        val categoryId = UUID.randomUUID()
        val request = ProductRequest("ANE-002", "Anel", null, 10, categoryId)

        `when`(productRepository.findBySku("ANE-002")).thenReturn(null)
        `when`(categoryRepository.findById(categoryId)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { productService.createProduct(request) }
    }

    @Test
    fun `updateProduct atualiza produto e valida estoque reservado e SKU duplicado`() {
        val productId = UUID.randomUUID()
        val category = activeCategory()
        val existing = sampleProduct(id = productId, category = category, totalQuantity = 20, reservedQuantity = 5)

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(existing))
        `when`(productRepository.existsBySkuAndIdNot("ANE-999", productId)).thenReturn(true)
        assertThrows(ConflictException::class.java) {
            productService.updateProduct(ProductRequest("ANE-999", "Anel", null, 10, category.id!!), productId)
        }

        `when`(productRepository.existsBySkuAndIdNot("ANE-001", productId)).thenReturn(false)
        assertThrows(ConflictException::class.java) {
            productService.updateProduct(ProductRequest("ANE-001", "Anel", null, 4, category.id!!), productId)
        }

        `when`(categoryRepository.findById(category.id!!)).thenReturn(Optional.of(category))
        `when`(productRepository.save(anyNonNull<Product>())).thenAnswer { it.getArgument<Product>(0) }

        val updated = productService.updateProduct(
            ProductRequest("ANE-001", "Anel Novo", "Desc", 30, category.id!!),
            productId
        )
        assertEquals("Anel Novo", updated.name)
        assertEquals(30, updated.totalQuantity)
    }

    @Test
    fun `deactivateProduct e reactivateProduct alteram status e rejeitam estado repetido`() {
        val id = UUID.randomUUID()
        val product = sampleProduct(id = id, isActive = true)
        `when`(productRepository.findById(id)).thenReturn(Optional.of(product))
        `when`(productRepository.save(anyNonNull<Product>())).thenAnswer { it.getArgument<Product>(0) }

        assertThrows(ConflictException::class.java) { productService.reactivateProduct(id) }

        val deactivated = productService.deactivateProduct(id)
        assertFalse(deactivated.isActive)

        assertThrows(ConflictException::class.java) { productService.deactivateProduct(id) }

        val reactivated = productService.reactivateProduct(id)
        assertTrue(reactivated.isActive)
    }

    @Test
    fun `getProductById lanca ResourceNotFoundException quando nao encontrado`() {
        val id = UUID.randomUUID()
        `when`(productRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { productService.getProductById(id) }
    }
}
