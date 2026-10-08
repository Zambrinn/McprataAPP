package com.mcpratapp.config


import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.AMQP
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.Jackson2JsonDecoder

@Configuration
class RabbitMQConfig {
    companion object {
        const val PAYMENT_EXCHANGE = "payment.exchange"
        const val PAYMENT_QUEUE = "payment.process.queue"
        const val PAYMENT_ROUTING_KEY = "payment.process"

        const val PAYMENT_DLQ_EXCHANGE = "payment.dql.exchange"
        const val PAYMENT_DLQ_QUEUE = "payment.process.dlq"
        const val PAYMENT_DLQ_ROUTING_KEY = "payment.process.dlq"
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: org.springframework.amqp.rabbit.connection.ConnectionFactory,
        messageConverter: MessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }

    @Bean
    fun paymentDLQQueue(): Queue {
        return QueueBuilder.durable(PAYMENT_DLQ_QUEUE).build()
    }

    @Bean
    fun paymentDLQExchange(): DirectExchange {
        return DirectExchange(PAYMENT_DLQ_EXCHANGE)
    }


    @Bean
    fun paymentDLQBinding(): Binding {
        return BindingBuilder
            .bind(paymentDLQQueue())
            .to(paymentDLQExchange())
            .with(PAYMENT_DLQ_ROUTING_KEY)
    }

    @Bean
    fun paymentQueue(): Queue {
        return QueueBuilder.durable(PAYMENT_QUEUE)
            .deadLetterExchange(PAYMENT_DLQ_EXCHANGE)
            .deadLetterRoutingKey(PAYMENT_DLQ_ROUTING_KEY)
            .build()
    }

    @Bean
    fun paymentExchange(): DirectExchange {
        return DirectExchange(PAYMENT_EXCHANGE)
    }

    @Bean
    fun paymentBinding(): Binding {
        return BindingBuilder
            .bind(paymentQueue())
            .to(paymentExchange())
            .with(PAYMENT_ROUTING_KEY)
    }
}