package com.togethertrip.notification.notification.service

import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode

@Component
class NotificationPayloadMapper {

    fun map(message: MainOutboxEventMessage): NotificationDisplay {
        val payload = message.payload
        return when (message.eventType) {
            "TRIP_PARTICIPANTS_ADDED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "여행에 초대되었습니다",
                body = "${payload.text("actorDisplayName") ?: "누군가"}님이 여행에 초대했습니다.",
                deeplink = tripDeeplink(payload),
            )

            "TRIP_PARTICIPANT_JOINED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "새 참여자가 들어왔습니다",
                body = "${payload.text("actorDisplayName") ?: "참여자"}님이 여행에 참여했습니다.",
                deeplink = tripDeeplink(payload),
            )

            "TRIP_PARTICIPANT_REMOVED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "여행 참여 상태가 변경되었습니다",
                body = "${payload.text("actorDisplayName") ?: "관리자"}님이 여행 참여자를 변경했습니다.",
                deeplink = tripDeeplink(payload),
            )

            "POST_CREATED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "새 게시글이 올라왔습니다",
                body = "${payload.text("actorDisplayName") ?: "누군가"}님이 ${payload.text("title") ?: "게시글"}을 작성했습니다.",
                deeplink = postDeeplink(payload),
            )

            "EXPENSE_POST_CREATED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "새 지출이 등록되었습니다",
                body = "${payload.text("actorDisplayName") ?: "누군가"}님이 ${payload.text("amount") ?: ""}${payload.text("currency") ?: ""} 지출을 등록했습니다.",
                deeplink = postDeeplink(payload),
            )

            "POST_COMMENT_CREATED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "새 댓글이 달렸습니다",
                body = "${payload.text("actorDisplayName") ?: "누군가"}님이 댓글을 남겼습니다.",
                deeplink = postDeeplink(payload),
            )

            "SETTLEMENT_CONFIRMED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "정산이 확정되었습니다",
                body = "여행 정산이 확정되었습니다.",
                deeplink = settlementDeeplink(payload),
            )

            "SETTLEMENT_TRANSFER_CONFIRMED_BY_SENDER" -> NotificationDisplay(
                title = payload.text("tripName") ?: "송금 확인 요청",
                body = "${payload.text("actorDisplayName") ?: "송금자"}님이 송금을 완료했다고 표시했습니다.",
                deeplink = settlementTransferDeeplink(payload),
            )

            "SETTLEMENT_TRANSFER_COMPLETED" -> NotificationDisplay(
                title = payload.text("tripName") ?: "송금이 완료되었습니다",
                body = "${payload.text("actorDisplayName") ?: "수금자"}님이 송금을 확인했습니다.",
                deeplink = settlementTransferDeeplink(payload),
            )

            else -> NotificationDisplay(
                title = "새 알림이 도착했습니다",
                body = "${message.aggregateType} ${message.eventType} 이벤트가 도착했습니다.",
                deeplink = aggregateDeeplink(message),
            )
        }
    }

    private fun tripDeeplink(payload: JsonNode): String? =
        payload.long("tripId")?.let { "togethertrip://trips/$it" }

    private fun postDeeplink(payload: JsonNode): String? {
        val tripId = payload.long("tripId") ?: return null
        val postId = payload.long("postId") ?: return "togethertrip://trips/$tripId"
        return "togethertrip://trips/$tripId/posts/$postId"
    }

    private fun settlementDeeplink(payload: JsonNode): String? {
        val tripId = payload.long("tripId") ?: return null
        val settlementId = payload.long("settlementId") ?: return "togethertrip://trips/$tripId/settlements"
        return "togethertrip://trips/$tripId/settlements/$settlementId"
    }

    private fun settlementTransferDeeplink(payload: JsonNode): String? {
        val tripId = payload.long("tripId") ?: return null
        val settlementId = payload.long("settlementId") ?: return "togethertrip://trips/$tripId/settlements"
        val transferId = payload.long("settlementTransferId")
            ?: return "togethertrip://trips/$tripId/settlements/$settlementId"
        return "togethertrip://trips/$tripId/settlements/$settlementId/transfers/$transferId"
    }

    private fun aggregateDeeplink(message: MainOutboxEventMessage): String? =
        when (message.aggregateType) {
            "TRIP" -> "togethertrip://trips/${message.aggregateId}"
            "POST" -> "togethertrip://posts/${message.aggregateId}"
            "SETTLEMENT" -> "togethertrip://settlements/${message.aggregateId}"
            else -> null
        }
}

data class NotificationDisplay(
    val title: String,
    val body: String,
    val deeplink: String?,
)

private fun JsonNode.text(fieldName: String): String? =
    path(fieldName).asStringOpt().orElse(null)?.takeIf { it.isNotBlank() }

private fun JsonNode.long(fieldName: String): Long? {
    val node = path(fieldName)
    return when {
        node.canConvertToLong() -> node.asLong()
        else -> null
    }
}
