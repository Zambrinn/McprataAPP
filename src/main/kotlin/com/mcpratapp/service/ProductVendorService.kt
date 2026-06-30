package com.mcpratapp.service

import com.mcpratapp.dto.request.ProductVendorRequest
import com.mcpratapp.dto.request.ProductVendorUpdateRequest
import com.mcpratapp.dto.response.ProductVendorResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.ProductVendor
import com.mcpratapp.model.Role
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class ProductVendorService (
    private val productVendorRepository: ProductVendorRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {
    fun createProductVendor(request: ProductVendorRequest): ProductVendorResponse {
        val product = productRepository.findByIdOrNull(request.productId)
            ?: throw ResourceNotFoundException("Produto não encontrado com id: ${request.productId}")

        if (!product.isActive) {
            throw ConflictException("Não é possível vincular um produto inativo.")
        }

        val vendor = userRepository.findByIdOrNull(request.vendorId)
            ?: throw ResourceNotFoundException("Vendedor não encontrado com id: ${request.vendorId}")

        if (vendor.status != UserStatus.ACTIVE) {
            throw ConflictException("Não é possível vincular produto a um vendedor inativo.")
        }

        if (vendor.role != Role.VENDOR) {
            throw ConflictException("Só é possível vincular produtos a usuários vendedores.")
        }

        if (productVendorRepository.existsByVendorIdAndProductId(request.vendorId, request.productId)) {
            throw ConflictException("Este vendedor já possui vínculo com este produto.")
        }

        val productVendor = ProductVendor(
            product = product,
            vendor = vendor,
            price = request.price,
            isActive = true
        )

        return productVendorRepository.save(productVendor).toResponse()
    }

    fun getAllProductVendors(): List<ProductVendorResponse> {
        return productVendorRepository.findAll().map { it.toResponse() }
    }

    fun getProductsByVendorId(vendorId: UUID): List<ProductVendorResponse> {
        userRepository.findByIdOrNull(vendorId)
            ?: throw ResourceNotFoundException("Vendedor não encontrado com id: $vendorId")

        return productVendorRepository.findByVendorId(vendorId).map { it.toResponse() }
    }

    fun getProductsByProductId(productId: UUID): List<ProductVendorResponse> {
        productRepository.findByIdOrNull(productId)
            ?: throw ResourceNotFoundException("Produto não encontrado com id: $productId")

        return productVendorRepository.findByProductId(productId).map { it.toResponse() }
    }

    fun getProductVendorById(id: UUID): ProductVendorResponse {
        val productVendor = productVendorRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Vínculo produto-vendedor não encontrado com id: $id")

        return productVendor.toResponse()
    }

    fun updateProductVendor(id: UUID, request: ProductVendorUpdateRequest): ProductVendorResponse {
        val productVendor = productVendorRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Vínculo produto-vendedor não encontrado com id: $id")

        productVendor.price = request.price
        productVendor.updatedAt = LocalDateTime.now()

        return productVendorRepository.save(productVendor).toResponse()
    }

    fun deactivateProductVendor(id: UUID): ProductVendorResponse {
        val productVendor = productVendorRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Vínculo produto-vendedor não encontrado com id: $id")

        if (!productVendor.isActive) {
            throw ConflictException("Vínculo produto-vendedor já está inativo.")
        }

        productVendor.isActive = false
        productVendor.updatedAt = LocalDateTime.now()

        return productVendorRepository.save(productVendor).toResponse()
    }

    fun restoreProductVendor(id: UUID): ProductVendorResponse {
        val productVendor = productVendorRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Vínculo produto-vendedor não encontrado com id: $id")

        if (productVendor.isActive) {
            throw ConflictException("Vínculo produto-vendedor já está ativo.")
        }

        productVendor.isActive = true
        productVendor.updatedAt = LocalDateTime.now()

        return productVendorRepository.save(productVendor).toResponse()
    }

    private fun ProductVendor.toResponse(): ProductVendorResponse {
        return ProductVendorResponse(
            id = this.id ?: throw IllegalStateException("Vínculo produto-vendedor salvo sem ID."),
            productId = this.product.id ?: throw IllegalStateException("Produto salvo sem ID."),
            vendorId = this.vendor.id ?: throw IllegalStateException("Vendedor salvo sem ID."),
            price = this.price,
            isActive = this.isActive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
