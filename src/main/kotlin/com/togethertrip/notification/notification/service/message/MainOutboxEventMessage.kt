package com.togethertrip.notification.notification.service.message

import tools.jackson.databind.JsonNode
import java.time.Instant

data class MainOutboxEventMessage(
    val id: Long,
    val aggregateType: String,
    val aggregateId: Long,
    val eventType: String,
    val payload: JsonNode,
) {
    fun recipientUserIds(): List<Long> {
        val recipients = payload.path("recipients")
        if (!recipients.isArray) {
            return emptyList()
        }

        return recipients.mapNotNull { recipient ->
            val userId = recipient.path("userId")
            when {
                userId.canConvertToLong() -> userId.asLong()
                else -> null
            }
        }.distinct()
    }

    fun occurredAt(): Instant? {
        val value = payload.path("occurredAt").asStringOpt().orElse(null) ?: return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}
