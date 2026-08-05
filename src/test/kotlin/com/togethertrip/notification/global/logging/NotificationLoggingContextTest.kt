package com.togethertrip.notification.global.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationLoggingContextTest {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `MDC 값에서 로그 인젝션 문자를 제거하고 길이를 제한한다`() {
        NotificationLoggingContext.putEventType("USER_CREATED\nforged=value${"x".repeat(120)}")

        val eventType = MDC.get(NotificationLoggingContext.EVENT_TYPE)

        assertEquals(100, eventType.length)
        assertEquals(false, eventType.contains('\n'))
        assertEquals(false, eventType.contains('='))
    }

    @Test
    fun `알림 범위만 정리하고 요청 범위는 유지한다`() {
        NotificationLoggingContext.putRequestId("request-1")
        NotificationLoggingContext.putUser("10")
        NotificationLoggingContext.putNotification("20")
        NotificationLoggingContext.putEventType("POST_CREATED")
        NotificationLoggingContext.putTargetUser("30")
        NotificationLoggingContext.putProvider("FCM")

        NotificationLoggingContext.clearNotificationScope()

        assertEquals("request-1", MDC.get(NotificationLoggingContext.REQUEST_ID))
        assertEquals("10", MDC.get(NotificationLoggingContext.USER_ID))
        assertNull(MDC.get(NotificationLoggingContext.NOTIFICATION_ID))
        assertNull(MDC.get(NotificationLoggingContext.EVENT_TYPE))
        assertNull(MDC.get(NotificationLoggingContext.TARGET_USER_ID))
        assertNull(MDC.get(NotificationLoggingContext.PROVIDER))
    }
}
