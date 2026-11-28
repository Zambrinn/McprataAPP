package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.ClientRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Client
import com.mcpratapp.repository.ClientRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ClientServiceTest {

    @Mock
    private lateinit var clientRepository: ClientRepository

    @InjectMocks
    private lateinit var clientService: ClientService

    private val validCpf = "52998224725"
    private val validCpfFormatted = "529.982.247-25"
    private val validCnpj = "11222333000181"
    private val validCnpjFormatted = "11.222.333/0001-81"

    private fun sampleClient(id: UUID = UUID.randomUUID(), isActive: Boolean = true) = Client(
        id = id,
        name = "Maria Silva",
        whatsappNumber = "5511999998888",
        email = "maria@test.com",
        address = "Rua das Flores, 100",
        cpf = validCpf,
        isActive = isActive
    )

    @Test
    fun `createClient cria cliente com CPF valido removendo mascara`() {
        val savedId = UUID.randomUUID()
        val request = ClientRequest(
            name = "Maria Silva",
            whatsappNumber = "5511999998888",
            email = "maria@test.com",
            address = "Rua A, 10",
            cpf = validCpfFormatted,
            cnpj = null,
            companyName = null
        )

        `when`(clientRepository.findByEmail(request.email)).thenReturn(null)
        `when`(clientRepository.findByWhatsappNumber(request.whatsappNumber)).thenReturn(null)
        `when`(clientRepository.findByCpf(validCpf)).thenReturn(null)
        `when`(clientRepository.save(anyNonNull<Client>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Client>(0)
            Client(
                id = savedId,
                name = arg.name,
                whatsappNumber = arg.whatsappNumber,
                email = arg.email,
                address = arg.address,
                companyName = arg.companyName,
                cpf = arg.cpf,
                cnpj = arg.cnpj,
                isActive = arg.isActive
            )
        }

        val response = clientService.createClient(request)

        assertEquals(savedId, response.id)
        assertEquals(validCpf, response.cpf)
        assertNull(response.cnpj)
        assertTrue(response.isActive)
    }

    @Test
    fun `createClient cria cliente com CNPJ valido removendo mascara`() {
        val savedId = UUID.randomUUID()
        val request = ClientRequest(
            name = "Joalheria Prata",
            whatsappNumber = "5511988887777",
            email = "contato@prata.com",
            address = "Av Central, 200",
            cpf = null,
            cnpj = validCnpjFormatted,
            companyName = "Prata LTDA"
        )

        `when`(clientRepository.findByEmail(request.email)).thenReturn(null)
        `when`(clientRepository.findByWhatsappNumber(request.whatsappNumber)).thenReturn(null)
        `when`(clientRepository.findByCnpj(validCnpj)).thenReturn(null)
        `when`(clientRepository.save(anyNonNull<Client>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Client>(0)
            Client(
                id = savedId,
                name = arg.name,
                whatsappNumber = arg.whatsappNumber,
                email = arg.email,
                address = arg.address,
                companyName = arg.companyName,
                cpf = arg.cpf,
                cnpj = arg.cnpj,
                isActive = arg.isActive
            )
        }

        val response = clientService.createClient(request)

        assertEquals(validCnpj, response.cnpj)
        assertNull(response.cpf)
    }

    @Test
    fun `createClient lanca ConflictException para email ou whatsapp duplicado`() {
        val request = ClientRequest(
            name = "Maria",
            whatsappNumber = "5511999998888",
            email = "maria@test.com",
            address = "Rua A",
            cpf = validCpf,
            cnpj = null,
            companyName = null
        )

        `when`(clientRepository.findByEmail(request.email)).thenReturn(sampleClient())
        assertThrows(ConflictException::class.java) { clientService.createClient(request) }

        `when`(clientRepository.findByEmail(request.email)).thenReturn(null)
        `when`(clientRepository.findByWhatsappNumber(request.whatsappNumber)).thenReturn(sampleClient())
        assertThrows(ConflictException::class.java) { clientService.createClient(request) }
    }

    @Test
    fun `createClient rejeita ausencia de documento ou envio simultaneo de CPF e CNPJ`() {
        val semDoc = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", null, null, null)
        val ambos = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", validCpf, validCnpj, null)

        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(semDoc) }
        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(ambos) }
    }

    @Test
    fun `createClient rejeita CPF invalido e CPF duplicado`() {
        val cpfRepetido = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", "11111111111", null, null)
        val cpfDigitoErrado = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", "52998224720", null, null)
        val cpfCurto = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", "123456", null, null)

        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(cpfRepetido) }
        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(cpfDigitoErrado) }
        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(cpfCurto) }

        val cpfValido = ClientRequest("Maria", "5511999998888", "m@t.com", "Rua A", validCpf, null, null)
        `when`(clientRepository.findByCpf(validCpf)).thenReturn(sampleClient())
        assertThrows(ConflictException::class.java) { clientService.createClient(cpfValido) }
    }

    @Test
    fun `createClient rejeita CNPJ invalido e CNPJ duplicado`() {
        val cnpjRepetido = ClientRequest("Empresa", "5511999998888", "e@t.com", "Rua A", null, "00000000000000", null)
        val cnpjDigitoErrado = ClientRequest("Empresa", "5511999998888", "e@t.com", "Rua A", null, "11222333000180", null)

        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(cnpjRepetido) }
        assertThrows(IllegalArgumentException::class.java) { clientService.createClient(cnpjDigitoErrado) }

        val cnpjValido = ClientRequest("Empresa", "5511999998888", "e@t.com", "Rua A", null, validCnpj, null)
        `when`(clientRepository.findByCnpj(validCnpj)).thenReturn(sampleClient())
        assertThrows(ConflictException::class.java) { clientService.createClient(cnpjValido) }
    }

    @Test
    fun `getClientById retorna cliente ou lanca ResourceNotFoundException`() {
        val id = UUID.randomUUID()
        val client = sampleClient(id)
        `when`(clientRepository.findById(id)).thenReturn(Optional.of(client))

        val found = clientService.getClientById(id)
        assertEquals(id, found.id)

        val missingId = UUID.randomUUID()
        `when`(clientRepository.findById(missingId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { clientService.getClientById(missingId) }
    }

    @Test
    fun `updateClient atualiza dados quando valido e bloqueia conflitos`() {
        val id = UUID.randomUUID()
        val existing = sampleClient(id)
        val request = ClientRequest(
            name = "Maria Atualizada",
            whatsappNumber = "5511977776666",
            email = "nova@test.com",
            address = "Rua B, 20",
            cpf = validCpf,
            cnpj = null,
            companyName = null
        )

        `when`(clientRepository.findById(id)).thenReturn(Optional.of(existing))
        `when`(clientRepository.existsByWhatsappNumberAndIdNot(request.whatsappNumber, id)).thenReturn(false)
        `when`(clientRepository.existsByEmailAndIdNot(request.email, id)).thenReturn(false)
        `when`(clientRepository.existsByCpfAndIdNot(validCpf, id)).thenReturn(false)
        `when`(clientRepository.save(anyNonNull<Client>())).thenAnswer { it.getArgument<Client>(0) }

        val updated = clientService.updateClient(id, request)
        assertEquals("Maria Atualizada", updated.name)
        assertEquals("nova@test.com", updated.email)

        `when`(clientRepository.existsByEmailAndIdNot(request.email, id)).thenReturn(true)
        assertThrows(ConflictException::class.java) { clientService.updateClient(id, request) }
    }

    @Test
    fun `deactivateClient e reactivateClient alternam estado e validam transicoes invalidas`() {
        val id = UUID.randomUUID()
        val activeClient = sampleClient(id, isActive = true)
        `when`(clientRepository.findById(id)).thenReturn(Optional.of(activeClient))
        `when`(clientRepository.save(anyNonNull<Client>())).thenAnswer { it.getArgument<Client>(0) }

        assertThrows(ConflictException::class.java) { clientService.reactivateClient(id) }

        val deactivated = clientService.deactivateClient(id)
        assertFalse(deactivated.isActive)

        assertThrows(ConflictException::class.java) { clientService.deactivateClient(id) }

        val reactivated = clientService.reactivateClient(id)
        assertTrue(reactivated.isActive)
    }
}
