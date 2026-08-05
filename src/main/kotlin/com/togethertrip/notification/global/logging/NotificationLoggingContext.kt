package com.togethertrip.notification.global.logging

import org.slf4j.MDC

object NotificationLoggingContext {
    const val REQUEST_ID = "requestId"
    const val USER_ID = "userId"
    const val NOTIFICATION_ID = "notificationId"
    const val EVENT_TYPE = "eventType"
    const val TARGET_USER_ID = "targetUserId"
    const val PROVIDER = "provider"

    fun putRequestId(requestId: String) = put(REQUEST_ID, requestId)

    fun putUser(userId: String?) = put(USER_ID, userId ?: "anonymous")

    fun putNotification(notificationId: String?) = putIfPresent(NOTIFICATION_ID, notificationId)

    fun putEventType(eventType: String?) = putIfPresent(EVENT_TYPE, eventType)

    fun putTargetUser(targetUserId: String?) = putIfPresent(TARGET_USER_ID, targetUserId)

    fun putProvider(provider: String?) = putIfPresent(PROVIDER, provider)

    fun clearNotificationScope() {
        MDC.remove(NOTIFICATION_ID)
        MDC.remove(EVENT_TYPE)
        MDC.remove(TARGET_USER_ID)
        MDC.remove(PROVIDER)
    }

    private fun putIfPresent(key: String, value: String?) {
        value?.takeIf { it.isNotBlank() }?.let { put(key, it) }
    }

    private fun put(key: String, value: String) {
        MDC.put(key, sanitize(value))
    }

    private fun sanitize(value: String): String =
        value
            .filter { it.isLetterOrDigit() || it in SAFE_PUNCTUATION }
            .take(MAX_VALUE_LENGTH)
            .ifBlank { "unknown" }

    private const val MAX_VALUE_LENGTH = 100
    private const val SAFE_PUNCTUATION = "-_.:@"
}
