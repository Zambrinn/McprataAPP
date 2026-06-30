package com.mcpratapp.controller

import com.mcpratapp.dto.request.ProductRequest
import com.mcpratapp.dto.response.ProductResponse
import com.mcpratapp.service.ProductService
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
@RequestMapping("/api/v1/products")
class ProductController (
    private val productService: ProductService
)   {
    @PostMapping
    fun createProduct(@Valid @RequestBody request: ProductRequest): ResponseEntity<ProductResponse> {
        val product = productService.createProduct(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(product)
    }

    @GetMapping
    fun getAllProducts(): ResponseEntity<List<ProductResponse>> {
        return ResponseEntity.ok(productService.getAllProducts())
    }

    @GetMapping("/{id}")
    fun getProductById(@Valid @PathVariable("id") productId: UUID): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.getProductById(productId))
    }

    @PutMapping("/{id}")
    fun updateProduct(@PathVariable("id") productId: UUID,
                      @Valid
                      @RequestBody
                      request: ProductRequest): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.updateProduct(request, productId))
    }

    @DeleteMapping("/{id}")
    fun deactivateProduct(@Valid @PathVariable("id") productId: UUID): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.deactivateProduct(productId))
    }

    @PutMapping("/{id}/restore")
    fun reactivateProduct(@Valid @PathVariable("id") productId: UUID): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.reactivateProduct(productId))
    }
}