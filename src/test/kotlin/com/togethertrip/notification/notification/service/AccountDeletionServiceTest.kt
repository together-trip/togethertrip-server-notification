package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.domain.PushDeliveryAttempt
import com.togethertrip.notification.notification.domain.PushDeliveryStatus
import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.repository.DeletedAccountRepository
import com.togethertrip.notification.notification.repository.NotificationRepository
import com.togethertrip.notification.notification.repository.PushDeliveryAttemptRepository
import com.togethertrip.notification.notification.repository.PushTokenRepository
import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountDeletionServiceTest(
    @Autowired private val service: AccountDeletionService,
    @Autowired private val deletedAccountRepository: DeletedAccountRepository,
    @Autowired private val notificationRepository: NotificationRepository,
    @Autowired private val pushTokenRepository: PushTokenRepository,
    @Autowired private val pushDeliveryAttemptRepository: PushDeliveryAttemptRepository,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `계정 삭제 이벤트는 사용자 알림 토큰 발송 이력을 삭제하고 tombstone을 남긴다`() {
        val deletedNotification = notificationRepository.save(notification(userId = 7L, sourceEventId = 101L))
        val otherNotification = notificationRepository.save(notification(userId = 8L, sourceEventId = 102L))
        val deletedToken = pushTokenRepository.save(PushToken(userId = 7L, token = "deleted-token"))
        val otherToken = pushTokenRepository.save(PushToken(userId = 8L, token = "other-token"))
        pushDeliveryAttemptRepository.save(
            PushDeliveryAttempt(
                notificationId = deletedNotification.id,
                pushTokenId = deletedToken.id,
                status = PushDeliveryStatus.SUCCESS,
            )
        )
        pushDeliveryAttemptRepository.save(
            PushDeliveryAttempt(
                notificationId = otherNotification.id,
                pushTokenId = otherToken.id,
                status = PushDeliveryStatus.SUCCESS,
            )
        )

        assertTrue(service.delete(accountDeletedMessage(sourceEventId = 301L, userId = 7L)))

        assertTrue(deletedAccountRepository.existsByUserId(7L))
        assertEquals(listOf(8L), notificationRepository.findAll().map { it.recipientUserId })
        assertEquals(listOf(8L), pushTokenRepository.findAll().map { it.userId })
        assertEquals(1, pushDeliveryAttemptRepository.count())
    }

    @Test
    fun `같은 사용자 또는 source event의 중복 삭제는 no-op이다`() {
        assertTrue(service.delete(accountDeletedMessage(sourceEventId = 301L, userId = 7L)))

        assertFalse(service.delete(accountDeletedMessage(sourceEventId = 301L, userId = 7L)))
        assertFalse(service.delete(accountDeletedMessage(sourceEventId = 302L, userId = 7L)))
        assertEquals(1, deletedAccountRepository.count())
    }

    @Test
    fun `계정 삭제 payload의 사용자와 aggregate가 다르면 거부한다`() {
        val message = accountDeletedMessage(sourceEventId = 301L, userId = 7L).copy(aggregateId = 8L)

        assertFailsWith<IllegalArgumentException> {
            service.delete(message)
        }
        assertEquals(0, deletedAccountRepository.count())
    }

    private fun accountDeletedMessage(sourceEventId: Long, userId: Long): MainOutboxEventMessage =
        MainOutboxEventMessage(
            id = sourceEventId,
            aggregateType = "USER",
            aggregateId = userId,
            eventType = "USER_ACCOUNT_DELETED",
            payload = objectMapper.readTree(
                """
                {
                  "eventVersion": 1,
                  "userId": $userId,
                  "occurredAt": "2026-07-28T12:00:00Z"
                }
                """.trimIndent()
            ),
        )

    private fun notification(userId: Long, sourceEventId: Long): Notification =
        Notification(
            sourceEventId = sourceEventId,
            recipientUserId = userId,
            eventType = "POST_CREATED",
            aggregateType = "POST",
            aggregateId = sourceEventId,
            payloadSnapshot = "{}",
            title = "알림",
            body = "본문",
            occurredAt = Instant.parse("2026-07-28T11:00:00Z"),
        )
}
