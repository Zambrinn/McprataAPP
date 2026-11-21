package com.mcpratapp.service

import com.mcpratapp.dto.request.ProductRequest
import com.mcpratapp.dto.response.CategoryResponse
import com.mcpratapp.dto.response.ProductResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Category
import com.mcpratapp.model.Product
import com.mcpratapp.repository.CategoryRepository
import com.mcpratapp.repository.ProductRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Transactional
@Service
class ProductService (
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
)   {
    fun createProduct(request: ProductRequest): ProductResponse {
        productRepository.findBySku(request.sku)?.let {
            throw ConflictException("Já existe um produto com esse SKU")
        }
        val category = resolveActiveCategory(request.categoryId)
        val product = Product(
            sku = request.sku,
            name = request.name,
            description = request.description,
            totalQuantity = request.totalQuantity,
            reservedQuantity = 0,
            isActive = true,
            category = category
        )
        return productRepository.save(product).toResponse()
    }

    fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll().map { it.toResponse() }
    }

    fun getProductById(id: UUID): ProductResponse {
        val product = productRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Não foi possivel encontrar o produto com id: $id")

        return product.toResponse()
    }

    fun updateProduct(request: ProductRequest, id: UUID): ProductResponse {
        val product = productRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Não foi possivel encontrar o produto com id: $id")

        if (productRepository.existsBySkuAndIdNot(request.sku, id)) {
            throw ConflictException("Já existe um outro produto com esse SKU")
        }

        if (request.totalQuantity < product.reservedQuantity) {
            throw ConflictException("A quantidade total não pode ser menor que a quantidade reservada")
        }
        val category = resolveActiveCategory(request.categoryId)
        product.sku = request.sku
        product.name = request.name
        product.totalQuantity = request.totalQuantity
        product.description = request.description
        product.category = category

        return productRepository.save(product).toResponse()
    }

    fun deactivateProduct(id: UUID): ProductResponse {
        val product = productRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Não foi possivel encontrar o produto com id: $id")

        if (!product.isActive) {
            throw ConflictException("O produto já está inativo")
        }

        product.isActive = false
        product.updatedAt = LocalDateTime.now()

        val savedProduct = productRepository.save(product)
        return savedProduct.toResponse()
    }

    fun reactivateProduct(id: UUID): ProductResponse {
        val product = productRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Não foi possivel encontrar o produto com id: $id")

        if (product.isActive) {
            throw ConflictException("O produto já está ativo")
        }

        product.isActive = true
        product.updatedAt = LocalDateTime.now()

        val savedProduct = productRepository.save(product)
        return savedProduct.toResponse()
    }

    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id!!,
            sku = this.sku,
            name = this.name,
            description = this.description,
            totalQuantity = this.totalQuantity,
            reservedQuantity = this.reservedQuantity,
            isActive = this.isActive,
            createdAt = this.createdAt,
            category = this.category?.let {
                CategoryResponse(id = it.id!!, name = it.name, skuPrefix = it.skuPrefix, isActive = it.isActive)
            }
        )
    }

    private fun resolveActiveCategory(categoryId: UUID): Category {
        val category = categoryRepository.findByIdOrNull(categoryId)
            ?: throw ResourceNotFoundException("Categoria não encontrada com id: $categoryId")

        if (!category.isActive) {
            throw ConflictException("Não é possível vincular produto a uma categoria inativa")
        }
        return category
    }
}
