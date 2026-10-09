package com.mcpratapp.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class SseService {
    private val logger = LoggerFactory.getLogger(SseService::class.java)

    private val emmiters = ConcurrentHashMap<UUID, SseEmitter>()
    fun subscribe(orderId: UUID): SseEmitter {
        val emmiter = SseEmitter(300_000L)

        emmiters[orderId] = emmiter
        logger.info("Cliente conectado ao SSE para envio do pedido: $orderId")

        emmiter.onCompletion {
            emmiters.remove(orderId)
            logger.info("Conexao SSE fechada para o pedido: $orderId")
        }
        emmiter.onTimeout {
            emmiters.remove(orderId)
            logger.warn("A conexao terminou para o pedido: $orderId")
        }

        emmiter.onError {
            emmiters.remove(orderId)
            logger.error("Erro na conexao SSE para pedido: $orderId")
        }

        try {
            emmiter.send(
                SseEmitter.event()
                    .name("INIT")
                    .data("Conectado com sucesso ao fluxo de pagamento do pedido: $orderId")
            )
        } catch (e: Exception) {
            emmiters.remove(orderId)
        }
        return emmiter
    }

    fun notifyPaymentConfirmed(orderId: UUID, payload: Any) {
        val emmiter = emmiters[orderId]
        if (emmiter != null) {
            try {
                emmiter.send(
                    SseEmitter.event()
                        .name("PAYMENT_CONFIRM")
                        .data(payload)
                )
                emmiter.complete()
                logger.info("Evento PAYMENT_CONFIRMED enviado com sucesso parea o pedido: $orderId")
            } catch (e: Exception) {
                logger.error("Falha ao enviar evento SSE para o pedido: $orderId", e)
                emmiters.remove(orderId)
            }
        } else {
            logger.warn("Nenhum cliente conectado via SSE aguardando pelo pedido: $orderId")
        }
    }
}