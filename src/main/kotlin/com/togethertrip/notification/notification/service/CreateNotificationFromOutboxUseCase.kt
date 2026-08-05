package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.push.NotificationPushDispatchService
import com.togethertrip.notification.notification.repository.NotificationRepository
import com.togethertrip.notification.notification.repository.DeletedAccountRepository
import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import com.togethertrip.notification.notification.service.result.CreateNotificationResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.ObjectMapper

@Service
class CreateNotificationFromOutboxUseCase(
    private val notificationRepository: NotificationRepository,
    private val notificationPayloadMapper: NotificationPayloadMapper,
    private val notificationPushDispatchService: NotificationPushDispatchService,
    private val objectMapper: ObjectMapper,
    private val deletedAccountRepository: DeletedAccountRepository,
) {

    @Transactional
    fun create(message: MainOutboxEventMessage): CreateNotificationResult {
        val recipientUserIds = message.recipientUserIds()
        if (recipientUserIds.isEmpty()) {
            return CreateNotificationResult(0)
        }

        val deletedUserIds = deletedAccountRepository.findDeletedUserIds(recipientUserIds)
        val activeRecipientUserIds = recipientUserIds.filterNot { it in deletedUserIds }
        if (activeRecipientUserIds.isEmpty()) {
            return CreateNotificationResult(0)
        }

        val existingRecipientUserIds = notificationRepository.findExistingRecipientUserIds(
            sourceEventId = message.id,
            recipientUserIds = activeRecipientUserIds,
        )
        val payloadSnapshot = objectMapper.writeValueAsString(message.payload)
        val display = notificationPayloadMapper.map(message)
        val notifications = activeRecipientUserIds
            .filterNot { it in existingRecipientUserIds }
            .map { recipientUserId ->
                Notification(
                    sourceEventId = message.id,
                    recipientUserId = recipientUserId,
                    eventType = message.eventType,
                    aggregateType = message.aggregateType,
                    aggregateId = message.aggregateId,
                    payloadSnapshot = payloadSnapshot,
                    title = display.title,
                    body = display.body,
                    deeplink = display.deeplink,
                    occurredAt = message.occurredAt(),
                )
            }

        if (notifications.isEmpty()) {
            return CreateNotificationResult(0)
        }

        val savedNotifications = notificationRepository.saveAll(notifications)
        dispatchPushAfterCommit(savedNotifications)
        return CreateNotificationResult(savedNotifications.size)
    }

    private fun dispatchPushAfterCommit(notifications: List<Notification>) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifications.forEach(notificationPushDispatchService::dispatch)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    notifications.forEach(notificationPushDispatchService::dispatch)
                }
            },
        )
    }
}
