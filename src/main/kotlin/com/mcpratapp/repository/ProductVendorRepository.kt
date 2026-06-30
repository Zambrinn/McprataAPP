package com.mcpratapp.repository

import com.mcpratapp.model.ProductVendor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductVendorRepository : JpaRepository<ProductVendor, UUID> {
    fun findByVendorIdAndProductId(vendorId: UUID, productId: UUID): ProductVendor?
    fun findByVendorId(vendorId: UUID): List<ProductVendor>
    fun findByProductId(productId: UUID): List<ProductVendor>
    fun existsByVendorIdAndProductId(vendorId: UUID, productId: UUID): Boolean
    fun existsByVendorIdAndProductIdAndIdNot(vendorId: UUID, productId: UUID, id: UUID): Boolean
}
