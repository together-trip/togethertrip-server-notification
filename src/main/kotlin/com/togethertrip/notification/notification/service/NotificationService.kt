package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.dto.NotificationResponse
import com.togethertrip.notification.notification.repository.NotificationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional(readOnly = true)
    fun getMyNotifications(userId: Long, limit: Int): List<NotificationResponse> {
        val pageSize = limit.coerceIn(1, MAX_NOTIFICATION_LIST_SIZE)
        return notificationRepository
            .findByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                recipientUserId = userId,
                pageable = PageRequest.of(0, pageSize),
            )
            .map(NotificationResponse::from)
    }

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long): NotificationResponse {
        val notification = findMine(userId, notificationId)
        if (notification.readAt == null) {
            notification.readAt = Instant.now()
        }
        return NotificationResponse.from(notification)
    }

    @Transactional
    fun markAllAsRead(userId: Long): Int {
        val notifications = notificationRepository
            .findByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                recipientUserId = userId,
                pageable = PageRequest.of(0, MAX_NOTIFICATION_LIST_SIZE),
            )
        val now = Instant.now()
        var updatedCount = 0
        notifications
            .filter { it.readAt == null }
            .forEach {
                it.readAt = now
                updatedCount += 1
            }
        return updatedCount
    }

    private fun findMine(userId: Long, notificationId: Long): Notification =
        notificationRepository.findByIdAndRecipientUserIdAndDeletedAtIsNull(
            id = notificationId,
            recipientUserId = userId,
        ) ?: throw NotificationNotFoundException(notificationId)

    private companion object {
        const val MAX_NOTIFICATION_LIST_SIZE = 100
    }
}

class NotificationNotFoundException(
    notificationId: Long,
) : RuntimeException("notification not found. notificationId=$notificationId")
