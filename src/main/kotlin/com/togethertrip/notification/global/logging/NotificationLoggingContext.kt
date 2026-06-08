package com.togethertrip.notification.global.logging

import org.slf4j.MDC

object NotificationLoggingContext {
    const val REQUEST_ID = "requestId"
    const val USER_ID = "userId"
    const val NOTIFICATION_ID = "notificationId"
    const val EVENT_TYPE = "eventType"
    const val TARGET_USER_ID = "targetUserId"
    const val PROVIDER = "provider"

    fun putUser(userId: String?) {
        MDC.put(USER_ID, userId?.takeIf { it.isNotBlank() } ?: "anonymous")
    }

    fun putNotification(notificationId: String?) {
        notificationId?.takeIf { it.isNotBlank() }?.let { MDC.put(NOTIFICATION_ID, it) }
    }

    fun putEventType(eventType: String?) {
        eventType?.takeIf { it.isNotBlank() }?.let { MDC.put(EVENT_TYPE, it) }
    }

    fun putTargetUser(targetUserId: String?) {
        targetUserId?.takeIf { it.isNotBlank() }?.let { MDC.put(TARGET_USER_ID, it) }
    }

    fun putProvider(provider: String?) {
        provider?.takeIf { it.isNotBlank() }?.let { MDC.put(PROVIDER, it) }
    }

    fun clearNotificationScope() {
        MDC.remove(NOTIFICATION_ID)
        MDC.remove(EVENT_TYPE)
        MDC.remove(TARGET_USER_ID)
        MDC.remove(PROVIDER)
    }
}
