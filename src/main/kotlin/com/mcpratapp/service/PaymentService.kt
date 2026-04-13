package com.mcpratapp.service

import com.mcpratapp.dto.response.PaymentResponse
import com.mcpratapp.model.Payment
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class PaymentService (
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) {
    fun getPaymentById(paymentId: UUID): PaymentResponse {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("O pagamento com id ${paymentId} não existe.") }
        return payment.toResponse()
    }

    fun getPaymentByOrderId(orderId: UUID): List<PaymentResponse> {
        orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }

        val payment = paymentRepository.findByOrderId(orderId)
        return payment?.let { listOf(it.toResponse()) } ?: emptyList()
    }

    private fun Payment.toResponse(): PaymentResponse {
        return PaymentResponse(
            id = this.id,
            orderId = this.order.id,
            method = this.method,
            status = this.status,
            amount = this.amount,
            paidAt = this.paidAt,
            createdAt = this.createdAt
        )
    }
}