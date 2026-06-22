package com.mcpratapp.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(
        exception: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildResponse(
            status = HttpStatus.NOT_FOUND,
            message = exception.message ?: "Recurso não encontrado.",
            path = request.requestURI
        )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        exception: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildResponse(
            status = HttpStatus.CONFLICT,
            message = exception.message ?: "Conflito na operação.",
            path = request.requestURI
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val errors = exception.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Valor inválido.")
        }

        val body = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Alguns dados estão inválidos, tente novamente.",
            path = request.requestURI,
            validationErrors = errors
        )

        return ResponseEntity.badRequest().body(body)
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        path: String
    ): ResponseEntity<ApiError> {
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path
        )

        return ResponseEntity.status(status).body(body)
    }
}