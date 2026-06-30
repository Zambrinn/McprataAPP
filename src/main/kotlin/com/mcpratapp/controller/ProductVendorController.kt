package com.mcpratapp.controller

import com.mcpratapp.dto.request.ProductVendorRequest
import com.mcpratapp.dto.request.ProductVendorUpdateRequest
import com.mcpratapp.dto.response.ProductVendorResponse
import com.mcpratapp.service.ProductVendorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/product-vendors")
class ProductVendorController(
    private val productVendorService: ProductVendorService
) {
    @PostMapping
    fun createProductVendor(
        @Valid @RequestBody request: ProductVendorRequest
    ): ResponseEntity<ProductVendorResponse> {
        val productVendor = productVendorService.createProductVendor(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(productVendor)
    }

    @GetMapping
    fun getAllProductVendors(): ResponseEntity<List<ProductVendorResponse>> {
        return ResponseEntity.ok(productVendorService.getAllProductVendors())
    }

    @GetMapping("/{id}")
    fun getProductVendorById(@PathVariable id: UUID): ResponseEntity<ProductVendorResponse> {
        return ResponseEntity.ok(productVendorService.getProductVendorById(id))
    }

    @GetMapping("/by-vendor/{vendorId}")
    fun getProductsByVendorId(
        @PathVariable vendorId: UUID
    ): ResponseEntity<List<ProductVendorResponse>> {
        return ResponseEntity.ok(productVendorService.getProductsByVendorId(vendorId))
    }

    @GetMapping("/by-product/{productId}")
    fun getProductsByProductId(
        @PathVariable productId: UUID
    ): ResponseEntity<List<ProductVendorResponse>> {
        return ResponseEntity.ok(productVendorService.getProductsByProductId(productId))
    }

    @PutMapping("/{id}")
    fun updateProductVendor(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ProductVendorUpdateRequest
    ): ResponseEntity<ProductVendorResponse> {
        return ResponseEntity.ok(productVendorService.updateProductVendor(id, request))
    }

    @DeleteMapping("/{id}")
    fun deactivateProductVendor(@PathVariable id: UUID): ResponseEntity<ProductVendorResponse> {
        return ResponseEntity.ok(productVendorService.deactivateProductVendor(id))
    }

    @PutMapping("/{id}/restore")
    fun restoreProductVendor(@PathVariable id: UUID): ResponseEntity<ProductVendorResponse> {
        return ResponseEntity.ok(productVendorService.restoreProductVendor(id))
    }
}
