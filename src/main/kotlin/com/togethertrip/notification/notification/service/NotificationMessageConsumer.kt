package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import com.togethertrip.notification.notification.service.message.ReceivedNotificationMessage
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
    private val accountDeletionService: AccountDeletionService,
    private val metrics: NotificationMessageConsumerMetrics,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.sqs.fixed-delay:PT3S}")
    fun poll() {
        notificationMessageQueue.receive().forEach(::handle)
    }

    fun handle(message: ReceivedNotificationMessage) {
        var accountDeletionEvent = false
        try {
            val outboxEvent = objectMapper.readValue(message.body, MainOutboxEventMessage::class.java)
            accountDeletionEvent = outboxEvent.eventType == NotificationEventContract.USER_ACCOUNT_DELETED
            val handledCount = if (outboxEvent.eventType == NotificationEventContract.USER_ACCOUNT_DELETED) {
                val deleted = accountDeletionService.delete(outboxEvent)
                metrics.recordAccountDeletion(
                    if (deleted) {
                        AccountDeletionConsumeOutcome.DELETED
                    } else {
                        AccountDeletionConsumeOutcome.DUPLICATE
                    }
                )
                logger.info(
                    "account deletion event consumed. sourceEventId={}, userId={}, outcome={}",
                    outboxEvent.id,
                    outboxEvent.aggregateId,
                    if (deleted) "deleted" else "duplicate",
                )
                if (deleted) 1 else 0
            } else {
                createNotificationFromOutboxUseCase.create(outboxEvent).createdCount
            }
            notificationMessageQueue.acknowledge(message)
            logger.info(
                "notification outbox message consumed. sourceEventId={}, handledCount={}",
                outboxEvent.id,
                handledCount,
            )
        } catch (exception: Exception) {
            if (accountDeletionEvent) {
                metrics.recordAccountDeletion(AccountDeletionConsumeOutcome.FAILED)
            }
            logger.warn(
                "notification outbox message consume failed. messageId={}",
                message.id,
                exception,
            )
        }
    }
}
