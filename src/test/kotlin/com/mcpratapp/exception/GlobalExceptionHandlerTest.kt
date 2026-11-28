package com.mcpratapp.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    private fun request(uri: String = "/api/v1/test"): MockHttpServletRequest =
        MockHttpServletRequest("GET", uri).apply { requestURI = uri }

    @Test
    fun `handleNotFound retorna 404 com mensagem e path`() {
        val response = handler.handleNotFound(ResourceNotFoundException("Item ausente"), request("/api/v1/items/1"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("Item ausente", response.body?.message)
        assertEquals("/api/v1/items/1", response.body?.path)
    }

    @Test
    fun `handleConflict retorna 409`() {
        val response = handler.handleConflict(ConflictException("Registro duplicado"), request())

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body?.status)
        assertEquals("Registro duplicado", response.body?.message)
    }

    @Test
    fun `handleIllegalArgument retorna 400`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("CPF inválido"), request())

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("CPF inválido", response.body?.message)
    }

    @Test
    fun `handleForbidden retorna 403`() {
        val response = handler.handleForbidden(ForbidenException("Sem permissão"), request())

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(403, response.body?.status)
        assertEquals("Sem permissão", response.body?.message)
    }

    @Test
    fun `handleValidation retorna 400 com mapa de erros por campo`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "target").apply {
            addError(FieldError("target", "email", "E-mail inválido"))
            addError(FieldError("target", "name", "Nome obrigatório"))
        }
        val exception = mock(MethodArgumentNotValidException::class.java)
        `when`(exception.bindingResult).thenReturn(bindingResult)

        val response = handler.handleValidation(exception, request("/api/v1/clients"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.validationErrors)
        assertEquals("E-mail inválido", response.body?.validationErrors?.get("email"))
        assertEquals("Nome obrigatório", response.body?.validationErrors?.get("name"))
    }

    @Test
    fun `handleGenericException retorna 500 com mensagem padrao`() {
        val response = handler.handleGenericException(RuntimeException("Erro inesperado"), request())

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(500, response.body?.status)
        assertEquals("Ocorreu um erro interno no servidor. Tente novamente mais tarde.", response.body?.message)
    }
}
