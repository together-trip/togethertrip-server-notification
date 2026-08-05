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
            var accountDeletionOutcome: AccountDeletionConsumeOutcome? = null
            val handledCount = if (outboxEvent.eventType == NotificationEventContract.USER_ACCOUNT_DELETED) {
                val deleted = accountDeletionService.delete(outboxEvent)
                accountDeletionOutcome =
                    if (deleted) {
                        AccountDeletionConsumeOutcome.DELETED
                    } else {
                        AccountDeletionConsumeOutcome.DUPLICATE
                    }
                if (deleted) 1 else 0
            } else {
                createNotificationFromOutboxUseCase.create(outboxEvent).createdCount
            }
            notificationMessageQueue.acknowledge(message)
            accountDeletionOutcome?.let { outcome ->
                recordAccountDeletionMetric(outcome)
                logger.info(
                    "account deletion event consumed. sourceEventId={}, userId={}, outcome={}",
                    outboxEvent.id,
                    outboxEvent.aggregateId,
                    outcome.tagValue,
                )
            }
            logger.info(
                "notification outbox message consumed. sourceEventId={}, handledCount={}",
                outboxEvent.id,
                handledCount,
            )
        } catch (exception: Exception) {
            if (accountDeletionEvent) {
                recordAccountDeletionMetric(AccountDeletionConsumeOutcome.FAILED)
            }
            logger.warn(
                "notification outbox message consume failed. messageId={}",
                message.id,
                exception,
            )
        }
    }

    private fun recordAccountDeletionMetric(outcome: AccountDeletionConsumeOutcome) {
        runCatching { metrics.recordAccountDeletion(outcome) }
            .onFailure { exception ->
                logger.warn(
                    "account deletion metric record failed. outcome={}",
                    outcome.tagValue,
                    exception,
                )
            }
    }
}
