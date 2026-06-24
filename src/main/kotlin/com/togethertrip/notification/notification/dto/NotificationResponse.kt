package com.togethertrip.notification.notification.dto

import com.togethertrip.notification.notification.domain.Notification
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val sourceEventId: Long,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: Long,
    val title: String,
    val body: String,
    val deeplink: String?,
    val occurredAt: Instant?,
    val readAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                sourceEventId = notification.sourceEventId,
                eventType = notification.eventType,
                aggregateType = notification.aggregateType,
                aggregateId = notification.aggregateId,
                title = notification.title,
                body = notification.body,
                deeplink = notification.deeplink,
                occurredAt = notification.occurredAt,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
