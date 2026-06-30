package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.exception.NotificationNotFoundException
import com.togethertrip.notification.notification.repository.NotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest(
    @Autowired private val notificationService: NotificationService,
    @Autowired private val notificationRepository: NotificationRepository,
) {

    @Test
    fun `내 알림만 최신순으로 조회한다`() {
        notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 1L))
        notificationRepository.save(sampleNotification(recipientUserId = 2L, sourceEventId = 2L))
        notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 3L))

        val result = notificationService.getMyNotifications(userId = 1L, limit = 100)

        assertEquals(2, result.size)
        assertEquals(listOf(3L, 1L), result.map { it.sourceEventId })
    }

    @Test
    fun `다른 사용자의 알림은 읽음 처리할 수 없다`() {
        val notification = notificationRepository.save(sampleNotification(recipientUserId = 2L, sourceEventId = 1L))

        assertThrows(NotificationNotFoundException::class.java) {
            notificationService.markAsRead(userId = 1L, notificationId = notification.id)
        }
    }

    @Test
    fun `내 알림을 읽음 처리한다`() {
        val notification = notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 1L))

        val result = notificationService.markAsRead(userId = 1L, notificationId = notification.id)

        assertNotNull(result.readAt)
    }

    @Test
    fun `읽지 않은 내 알림 개수만 조회한다`() {
        val unread = notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 1L))
        notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 2L))
        notificationRepository.save(sampleNotification(recipientUserId = 2L, sourceEventId = 3L))
        notificationService.markAsRead(userId = 1L, notificationId = unread.id)
        val deleted = notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 4L))
        notificationService.deleteNotification(userId = 1L, notificationId = deleted.id)

        val result = notificationService.countUnreadNotifications(userId = 1L)

        assertEquals(1L, result)
    }

    @Test
    fun `내 알림을 삭제하면 목록에서 제외한다`() {
        val notification = notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 1L))

        notificationService.deleteNotification(userId = 1L, notificationId = notification.id)

        val result = notificationService.getMyNotifications(userId = 1L, limit = 100)
        val deleted = notificationRepository.findById(notification.id).orElseThrow()

        assertEquals(emptyList<Notification>(), result)
        assertNotNull(deleted.deletedAt)
    }

    @Test
    fun `다른 사용자의 알림은 삭제할 수 없다`() {
        val notification = notificationRepository.save(sampleNotification(recipientUserId = 2L, sourceEventId = 1L))

        assertThrows(NotificationNotFoundException::class.java) {
            notificationService.deleteNotification(userId = 1L, notificationId = notification.id)
        }

        val notDeleted = notificationRepository.findById(notification.id).orElseThrow()
        assertNull(notDeleted.deletedAt)
    }

    @Test
    fun `삭제된 알림은 읽음 처리할 수 없다`() {
        val notification = notificationRepository.save(sampleNotification(recipientUserId = 1L, sourceEventId = 1L))
        notificationService.deleteNotification(userId = 1L, notificationId = notification.id)

        assertThrows(NotificationNotFoundException::class.java) {
            notificationService.markAsRead(userId = 1L, notificationId = notification.id)
        }
    }

    private fun sampleNotification(recipientUserId: Long, sourceEventId: Long): Notification =
        Notification(
            sourceEventId = sourceEventId,
            recipientUserId = recipientUserId,
            eventType = "POST_CREATED",
            aggregateType = "POST",
            aggregateId = sourceEventId,
            payloadSnapshot = "{}",
            title = "title $sourceEventId",
            body = "body",
            deeplink = "togethertrip://posts/$sourceEventId",
        )
}
