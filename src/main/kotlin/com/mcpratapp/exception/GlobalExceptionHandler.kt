package com.mcpratapp.exception

import jakarta.servlet.http.HttpServletRequest
import org.aspectj.bridge.Message
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(
        exception: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        logger.warn("Recurso nao encontrado no path [{}]: {}", request.requestURI)
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
        logger.warn("Conflito de negócio no path [{}]: {}", request.requestURI, exception.message)
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

        logger.warn("Falha de validacao no path [{}]: campos={}", request.requestURI, errors.keys)

        val body = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Alguns dados estão inválidos, tente novamente.",
            path = request.requestURI,
            validationErrors = errors
        )

        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        exception: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        logger.error("Erro interno nao tratado no path [{}]: {}", request.requestURI, exception.message, exception)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
            path = request.requestURI
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exception: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: "Requisição inválida.",
            path = request.requestURI
        )
    }

    @ExceptionHandler(ForbidenException::class)
    fun handleForbidden(
        exception: ForbidenException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildResponse(
            status = HttpStatus.FORBIDDEN,
            message = exception.message ?: "Acesso negado.",
            path = request.requestURI
        )
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