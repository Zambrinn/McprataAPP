package com.mcpratapp.service

import com.mcpratapp.dto.response.ClientResponse
import com.mcpratapp.dto.request.ClientRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Client
import com.mcpratapp.repository.ClientRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class ClientService (
    private val clientRepository: ClientRepository
) {
    fun createClient(request: ClientRequest): ClientResponse {
        clientRepository.findByEmail(request.email)?.let {
            throw ConflictException("Já existe um cliente com esse email cadastrado.")
        }
        clientRepository.findByWhatsappNumber(request.whatsappNumber)?.let {
            throw ConflictException("Já existe um cliente com esse número de telefone.")
        }

        val cleanCpf = request.cpf?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
        val cleanCnpj = request.cnpj?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }

        if (cleanCpf == null && cleanCnpj == null) {
            throw IllegalArgumentException("É obrigatório informar CPF ou CNPJ.")
        }
        if (cleanCpf != null && cleanCnpj != null) {
            throw IllegalArgumentException("Informe apenas CPF ou apenas CNPJ.")
        }

        if (cleanCpf != null) {
            if (!isValidCPF(cleanCpf)) {
                throw IllegalArgumentException("CPF inválido.")
            }
            clientRepository.findByCpf(cleanCpf)?.let {
                throw ConflictException("Já existe um cliente com esse CPF cadastrado.")
            }
        }

        if (cleanCnpj != null) {
            if (!isValidCNPJ(cleanCnpj)) {
                throw IllegalArgumentException("CNPJ inválido.")
            }
            clientRepository.findByCnpj(cleanCnpj)?.let {
                throw ConflictException("Já existe um cliente com esse CNPJ cadastrado.")
            }
        }
        
        val clientToSave = Client(
            name = request.name,
            whatsappNumber = request.whatsappNumber,
            email = request.email,
            address = request.address,
            companyName = request.companyName,
            cpf = cleanCpf,
            cnpj = cleanCnpj,
            isActive = true
        )

        val savedClient = clientRepository.save(clientToSave)
        return savedClient.toResponse()
    }

    fun getAllClients(): List<ClientResponse> {
        val foundClients = clientRepository.findAll()
        return foundClients.map { it.toResponse() }
    }

    fun getClientById(id: UUID): ClientResponse {
        val client = clientRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Cliente não encontrado com id: $id")

        return client.toResponse()
    }

    fun updateClient(id: UUID, request: ClientRequest): ClientResponse {
        val existingClient = clientRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Cliente não encontrado com id: $id")

        if (clientRepository.existsByWhatsappNumberAndIdNot(request.whatsappNumber, id)) {
            throw ConflictException("Já existe outro cliente com esse número de whatsapp")
        }

        if (clientRepository.existsByEmailAndIdNot(request.email, id)) {
            throw ConflictException("Já existe outro cliente com esse e-mail.")
        }

        val cleanCpf = request.cpf?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
        val cleanCnpj = request.cnpj?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }

        if (cleanCpf == null && cleanCnpj == null) {
            throw IllegalArgumentException("É obrigatório informar CPF ou CNPJ.")
        }
        if (cleanCpf != null && cleanCnpj != null) {
            throw IllegalArgumentException("Informe apenas CPF ou apenas CNPJ.")
        }

        if (cleanCpf != null) {
            if (!isValidCPF(cleanCpf)) {
                throw IllegalArgumentException("CPF inválido.")
            }
            if (clientRepository.existsByCpfAndIdNot(cleanCpf, id)) {
                throw ConflictException("Já existe outro cliente com esse CPF cadastrado.")
            }
        }

        if (cleanCnpj != null) {
            if (!isValidCNPJ(cleanCnpj)) {
                throw IllegalArgumentException("CNPJ inválido.")
            }
            if (clientRepository.existsByCnpjAndIdNot(cleanCnpj, id)) {
                throw ConflictException("Já existe outro cliente com esse CNPJ cadastrado.")
            }
        }

        existingClient.name = request.name
        existingClient.whatsappNumber = request.whatsappNumber
        existingClient.email = request.email
        existingClient.address = request.address
        existingClient.companyName = request.companyName
        existingClient.cpf = cleanCpf
        existingClient.cnpj = cleanCnpj
        existingClient.updatedAt = LocalDateTime.now()

        return clientRepository.save(existingClient).toResponse()
    }

    fun deactivateClient(id: UUID): ClientResponse {
        val existingClient = clientRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Cliente não encontrado com id: $id")

        if (!existingClient.isActive) {
            throw ConflictException("Cliente já está inativo")
        }

        existingClient.isActive = false
        existingClient.updatedAt = LocalDateTime.now()

        val savedClient = clientRepository.save(existingClient)
        return savedClient.toResponse()
    }

    fun reactivateClient(id: UUID): ClientResponse {
        val existingClient = clientRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Cliente não encontrado com id: $id")

        if (existingClient.isActive) {
            throw ConflictException("O usuário já está ativo")
        }

        existingClient.isActive = true
        existingClient.updatedAt = LocalDateTime.now()

        return clientRepository.save(existingClient).toResponse()
    }

    private fun isValidCPF(cpf: String): Boolean {
        val clearCpf = cpf.filter { it.isDigit() }

        if (clearCpf.length != 11) return false

        if (clearCpf.all { it == clearCpf[0] }) return false

        val sum1 = clearCpf.take(9).mapIndexed { i, c ->
            (10 - i) * c.digitToInt()
        }.sum()

        val firstVerifyingDigit = if (sum1 % 11 < 2) 0 else 11 - (sum1 % 11)

        val sum2 = clearCpf.take(10).mapIndexed { i, c ->
            (11 - i) * c.digitToInt()
        }.sum()

        val secondVerifyingDigit = if (sum2 % 11 < 2) 0 else 11 - (sum2 % 11)

        return clearCpf[9].digitToInt() == firstVerifyingDigit &&
                clearCpf[10].digitToInt() == secondVerifyingDigit
    }

    private fun isValidCNPJ(cnpj: String): Boolean {
        val clearCnpj = cnpj.filter { it.isDigit() }

        if (clearCnpj.length != 14) return false

        if (clearCnpj.all { it == clearCnpj[0] }) return false

        val weights1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val sum1 = clearCnpj.take(12).mapIndexed { i, c ->
            c.digitToInt() * weights1[i]
        }.sum()

        val remainder1 = sum1 % 11
        val firstVerifyingDigit = if (remainder1 < 2) 0 else 11 - remainder1

        val weights2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val sum2 = clearCnpj.take(13).mapIndexed { i, c ->
            c.digitToInt() * weights2[i]
        }.sum()

        val remainder2 = sum2 % 11
        val secondVerifyingDigit = if (remainder2 < 2) 0 else 11 - remainder2

        return clearCnpj[12].digitToInt() == firstVerifyingDigit &&
                clearCnpj[13].digitToInt() == secondVerifyingDigit
    }

    private fun Client.toResponse(): ClientResponse {
        return ClientResponse(
            id = this.id ?: throw IllegalStateException("Cliente salvo sem id"),
            name = this.name,
            whatsappNumber = this.whatsappNumber,
            email = this.email,
            address = this.address,
            companyName = this.companyName,
            cpf = this.cpf,
            cnpj = this.cnpj,
            isActive = this.isActive,
            createdAt = this.createdAt,
        )
    }
}