package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class CreateNotificationFromOutboxUseCase(
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun create(message: MainOutboxEventMessage): CreateNotificationResult {
        val recipientUserIds = message.recipientUserIds()
        if (recipientUserIds.isEmpty()) {
            return CreateNotificationResult(0)
        }

        val existingRecipientUserIds = notificationRepository.findExistingRecipientUserIds(
            sourceEventId = message.id,
            recipientUserIds = recipientUserIds,
        )
        val payloadSnapshot = objectMapper.writeValueAsString(message.payload)
        val notifications = recipientUserIds
            .filterNot { it in existingRecipientUserIds }
            .map { recipientUserId ->
                Notification(
                    sourceEventId = message.id,
                    recipientUserId = recipientUserId,
                    eventType = message.eventType,
                    aggregateType = message.aggregateType,
                    aggregateId = message.aggregateId,
                    payloadSnapshot = payloadSnapshot,
                    occurredAt = message.occurredAt(),
                )
            }

        if (notifications.isEmpty()) {
            return CreateNotificationResult(0)
        }

        return CreateNotificationResult(notificationRepository.saveAll(notifications).size)
    }
}

data class CreateNotificationResult(
    val createdCount: Int,
)
