package com.mcpratapp.controller

import com.mcpratapp.dto.request.CategoryRequest
import com.mcpratapp.dto.response.CategoryResponse
import com.mcpratapp.service.CategoryService
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
@RequestMapping("/api/v1/categories")
class CategoryController (
    private val categoryService: CategoryService
) {
    @GetMapping
    fun getAll(): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryService.getAllActiveCategories())

    @PostMapping
    fun create(@Valid @RequestBody request: CategoryRequest): ResponseEntity<CategoryResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request))
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: CategoryRequest): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.updateCategory(id, request))
    }

    @DeleteMapping("/{id}")
    fun deactivate(@PathVariable id: UUID): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.deactivateCategory(id))
    }
}