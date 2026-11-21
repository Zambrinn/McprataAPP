package com.mcpratapp.service

import com.mcpratapp.dto.request.CategoryRequest
import com.mcpratapp.dto.response.CategoryResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Category
import com.mcpratapp.repository.CategoryRepository
import com.mcpratapp.repository.ProductRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Transactional
@Service
class CategoryService (
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) {
    fun createCategory(request: CategoryRequest): CategoryResponse {
        val cleanName = request.name.trim()
        categoryRepository.findByNameIgnoreCase(cleanName)?.let {
            throw ConflictException("Já existe uma categoria com o nome '$cleanName'")
        }

        val prefix = request.skuPrefix?.trim()?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: cleanName.take(3).uppercase()

        val category = Category(name = cleanName, skuPrefix = prefix)

        return categoryRepository.save(category).toResponse()
    }

    fun getAllActiveCategories(): List<CategoryResponse> =  categoryRepository.findAllByIsActiveTrueOrderByNameAsc()
        .map { it.toResponse() }

    fun updateCategory(id: UUID, request: CategoryRequest): CategoryResponse {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Categoria nao encontrada: $id")

        val cleanName = request.name.trim()
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw ConflictException("Já existe outra categoria com o nome '$cleanName'")
        }

        category.name = cleanName
        category.skuPrefix = request.skuPrefix?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: category.skuPrefix
        category.updatedAt = LocalDateTime.now()

        return categoryRepository.save(category).toResponse()
    }

    fun deactivateCategory(id: UUID): CategoryResponse {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Categoria nao encontrada: $id")

        if (productRepository.existsByCategoryIdAndIsActiveTrue(id)) {
            throw ConflictException("Nao é possivel desativar uma categoria vinculada a produtos ativos")
        }

        category.isActive = false
        category.updatedAt = LocalDateTime.now()
        return categoryRepository.save(category).toResponse()
    }

    private fun Category.toResponse() = CategoryResponse(
        id = this.id!!,
        name = this.name,
        skuPrefix = this.skuPrefix,
        isActive = this.isActive
    )
}