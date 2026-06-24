package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.CreateNotificationFromOutboxUseCase
import com.togethertrip.notification.notification.service.MainOutboxEventMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@Component
class SqsNotificationMessageConsumer(
    private val sqsClient: SqsClient,
    private val properties: SqsNotificationConsumerProperties,
    private val objectMapper: ObjectMapper,
    private val createNotificationFromOutboxUseCase: CreateNotificationFromOutboxUseCase,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.sqs.fixed-delay:PT3S}")
    fun poll() {
        if (!properties.enabled || !properties.hasQueueUrl()) {
            return
        }

        val messages = sqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl)
                .maxNumberOfMessages(properties.maxMessages.coerceIn(1, 10))
                .waitTimeSeconds(properties.waitTime.toSeconds().toInt().coerceIn(0, 20))
                .visibilityTimeout(properties.visibilityTimeout.toSeconds().toInt().coerceAtLeast(0))
                .build(),
        ).messages()

        messages.forEach(::handle)
    }

    fun handle(message: Message) {
        try {
            val outboxEvent = objectMapper.readValue(message.body(), MainOutboxEventMessage::class.java)
            val result = createNotificationFromOutboxUseCase.create(outboxEvent)
            delete(message)
            logger.info(
                "notification outbox message consumed. sourceEventId={}, createdCount={}",
                outboxEvent.id,
                result.createdCount,
            )
        } catch (exception: Exception) {
            logger.warn(
                "notification outbox message consume failed. messageId={}",
                message.messageId(),
                exception,
            )
        }
    }

    private fun delete(message: Message) {
        sqsClient.deleteMessage(
            DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl)
                .receiptHandle(message.receiptHandle())
                .build(),
        )
    }
}
