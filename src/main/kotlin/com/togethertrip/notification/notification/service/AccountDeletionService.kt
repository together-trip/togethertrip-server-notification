package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.DeletedAccount
import com.togethertrip.notification.notification.repository.DeletedAccountRepository
import com.togethertrip.notification.notification.repository.NotificationRepository
import com.togethertrip.notification.notification.repository.PushDeliveryAttemptRepository
import com.togethertrip.notification.notification.repository.PushTokenRepository
import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AccountDeletionService(
    private val deletedAccountRepository: DeletedAccountRepository,
    private val notificationRepository: NotificationRepository,
    private val pushTokenRepository: PushTokenRepository,
    private val pushDeliveryAttemptRepository: PushDeliveryAttemptRepository,
) {
    @Transactional
    fun delete(message: MainOutboxEventMessage): Boolean {
        val payload = validate(message)
        if (
            deletedAccountRepository.existsByUserId(payload.userId) ||
            deletedAccountRepository.existsBySourceEventId(message.id)
        ) {
            return false
        }
        deletedAccountRepository.save(
            DeletedAccount(
                userId = payload.userId,
                sourceEventId = message.id,
                deletedAt = payload.occurredAt,
            )
        )

        val notificationIds = notificationRepository.findIdsByRecipientUserId(payload.userId)
        val pushTokenIds = pushTokenRepository.findIdsByUserId(payload.userId)
        if (notificationIds.isNotEmpty()) {
            pushDeliveryAttemptRepository.deleteAllByNotificationIds(notificationIds)
        }
        if (pushTokenIds.isNotEmpty()) {
            pushDeliveryAttemptRepository.deleteAllByPushTokenIds(pushTokenIds)
        }
        notificationRepository.deleteAllByRecipientUserId(payload.userId)
        pushTokenRepository.deleteAllByUserId(payload.userId)
        return true
    }

    private fun validate(message: MainOutboxEventMessage): AccountDeletedPayload {
        require(message.eventType == NotificationEventContract.USER_ACCOUNT_DELETED) {
            "unsupported account lifecycle event"
        }
        require(message.aggregateType == NotificationEventContract.AGGREGATE_USER) {
            "account deletion aggregateType must be USER"
        }
        val eventVersion = message.payload.path("eventVersion")
        val userId = message.payload.path("userId")
        val occurredAt = message.payload.path("occurredAt").asStringOpt().orElse(null)
        require(eventVersion.canConvertToInt() && eventVersion.asInt() == 1) {
            "unsupported account deletion event version"
        }
        require(userId.canConvertToLong() && userId.asLong() > 0) {
            "account deletion userId is required"
        }
        require(userId.asLong() == message.aggregateId) {
            "account deletion userId must match aggregateId"
        }
        val parsedOccurredAt = requireNotNull(
            occurredAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ) { "account deletion occurredAt is invalid" }

        return AccountDeletedPayload(userId.asLong(), parsedOccurredAt)
    }

    private data class AccountDeletedPayload(
        val userId: Long,
        val occurredAt: Instant,
    )
}
