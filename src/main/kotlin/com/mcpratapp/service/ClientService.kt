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
        
        val clientToSave = Client(
            name = request.name,
            whatsappNumber = request.whatsappNumber,
            email = request.email,
            address = request.address,
            companyName = request.companyName,
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

        existingClient.name = request.name
        existingClient.whatsappNumber = request.whatsappNumber
        existingClient.email = request.email
        existingClient.address = request.address
        existingClient.companyName = request.companyName
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

    private fun Client.toResponse(): ClientResponse {
        return ClientResponse(
            id = this.id,
            name = this.name,
            whatsappNumber = this.whatsappNumber,
            email = this.email,
            address = this.address,
            companyName = this.companyName,
            isActive = this.isActive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}