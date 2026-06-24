package com.togethertrip.notification.notification.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class NotificationMessageConsumer(
    private val notificationMessageQueue: NotificationMessageQueue,
    private val objectMapper: ObjectMapper,
    private val createNotificationFromOutboxUseCase: CreateNotificationFromOutboxUseCase,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.sqs.fixed-delay:PT3S}")
    fun poll() {
        notificationMessageQueue.receive().forEach(::handle)
    }

    fun handle(message: ReceivedNotificationMessage) {
        try {
            val outboxEvent = objectMapper.readValue(message.body, MainOutboxEventMessage::class.java)
            val result = createNotificationFromOutboxUseCase.create(outboxEvent)
            notificationMessageQueue.acknowledge(message)
            logger.info(
                "notification outbox message consumed. sourceEventId={}, createdCount={}",
                outboxEvent.id,
                result.createdCount,
            )
        } catch (exception: Exception) {
            logger.warn(
                "notification outbox message consume failed. messageId={}",
                message.id,
                exception,
            )
        }
    }
}
