package com.mcpratapp.controller

import com.mcpratapp.dto.response.ClientResponse
import com.mcpratapp.dto.request.ClientRequest
import com.mcpratapp.service.ClientService
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
@RequestMapping("/api/v1/clients")
class ClientController (
    private val clientService: ClientService
) {
    @PostMapping
    fun createClient(@Valid @RequestBody request: ClientRequest): ResponseEntity<ClientResponse> {
        val client = clientService.createClient(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(client)
    }

    @GetMapping
    fun getAllClients(): ResponseEntity<List<ClientResponse>> {
        return ResponseEntity.ok(clientService.getAllClients())
    }

    @GetMapping("/{id}")
    fun getClientById(@PathVariable("id") clientId: UUID): ResponseEntity<ClientResponse> {
        return ResponseEntity.ok(clientService.getClientById(clientId))
    }

    @PutMapping("/{id}")
    fun updateClient(
                     @PathVariable("id")
                     clientId: UUID,
                     @Valid
                     @RequestBody request: ClientRequest): ResponseEntity<ClientResponse> {
        val updatedClient = clientService.updateClient(clientId, request)
        return ResponseEntity.status(HttpStatus.OK).body(updatedClient)
    }

    @DeleteMapping("/{id}")
    fun deactivateClient(@Valid @PathVariable("id") clientId: UUID): ResponseEntity<ClientResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(clientService.deactivateClient(clientId))
    }
}