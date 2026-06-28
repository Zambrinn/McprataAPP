package com.mcpratapp.repository

import com.mcpratapp.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {
    fun findBySku(sku: String): Product?
    fun existsBySkuAndIdNot(sku: String, id: UUID): Boolean
}