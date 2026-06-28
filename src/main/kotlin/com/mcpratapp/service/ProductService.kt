package com.mcpratapp.service

import com.mcpratapp.dto.request.ProductRequest
import com.mcpratapp.dto.response.ProductResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Product
import com.mcpratapp.repository.ProductRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Transactional
@Service
class ProductService (
    private val productRepository: ProductRepository
)   {
    fun createProduct(request: ProductRequest): ProductResponse {
        productRepository.findBySku(request.sku)?.let {
            throw ConflictException("Já existe um produto com esse SKU")
        }

        val product = Product(
            sku = request.sku,
            name = request.name,
            description = request.description,
            totalQuantity = request.totalQuantity,
            reservedQuantity = 0,
            isActive = true,
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

        product.sku = request.sku
        product.name = request.name
        product.totalQuantity = request.totalQuantity
        product.description = request.description

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
            createdAt = this.createdAt
        )
    }
}