package com.togethertrip.notification.notification.push

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.domain.PushDeliveryAttempt
import com.togethertrip.notification.notification.domain.PushDeliveryStatus
import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.repository.PushDeliveryAttemptRepository
import com.togethertrip.notification.notification.repository.PushTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationPushDispatchServiceTest {

    private val pushTokenRepository = mock<PushTokenRepository>()
    private val pushDeliveryAttemptRepository = mock<PushDeliveryAttemptRepository>()
    private val pushNotificationSender = mock<PushNotificationSender>()
    private val service = NotificationPushDispatchService(
        pushTokenRepository = pushTokenRepository,
        pushDeliveryAttemptRepository = pushDeliveryAttemptRepository,
        pushNotificationSender = pushNotificationSender,
    )

    @Test
    fun `invalid token 결과는 token을 비활성화하고 delivery attempt를 기록한다`() {
        val pushToken = PushToken(userId = 1L, token = "sensitive-token")
        pushToken.id = 10L
        whenever(pushTokenRepository.findAllByUserIdAndActiveIsTrueAndDeletedAtIsNull(1L))
            .thenReturn(listOf(pushToken))
        whenever(pushNotificationSender.send(any()))
            .thenReturn(PushNotificationResult.invalidToken("fcm invalid token"))

        service.dispatch(sampleNotification())

        val attemptCaptor = argumentCaptor<PushDeliveryAttempt>()
        verify(pushDeliveryAttemptRepository).save(attemptCaptor.capture())
        assertFalse(pushToken.active)
        assertEquals(PushDeliveryStatus.INVALID_TOKEN, attemptCaptor.firstValue.status)
    }

    @Test
    fun `발송 성공은 success delivery attempt를 기록한다`() {
        val pushToken = PushToken(userId = 1L, token = "sensitive-token")
        pushToken.id = 10L
        whenever(pushTokenRepository.findAllByUserIdAndActiveIsTrueAndDeletedAtIsNull(1L))
            .thenReturn(listOf(pushToken))
        whenever(pushNotificationSender.send(any()))
            .thenReturn(PushNotificationResult.success("projects/test/messages/1"))

        service.dispatch(sampleNotification())

        val attemptCaptor = argumentCaptor<PushDeliveryAttempt>()
        verify(pushDeliveryAttemptRepository).save(attemptCaptor.capture())
        assertEquals(PushDeliveryStatus.SUCCESS, attemptCaptor.firstValue.status)
    }

    private fun sampleNotification(): Notification =
        Notification(
            sourceEventId = 1L,
            recipientUserId = 1L,
            eventType = "POST_CREATED",
            aggregateType = "POST",
            aggregateId = 10L,
            payloadSnapshot = "{}",
            title = "title",
            body = "body",
            deeplink = "togethertrip://posts/10",
        ).also { it.id = 100L }
}
