package com.mcpratapp.repository

import com.mcpratapp.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findByNameIgnoreCase(name: String): Category?
    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean
    fun findAllByIsActiveTrueOrderByNameAsc(): List<Category>
}