package com.togethertrip.notification.notification.push

interface PushNotificationSender {

    fun send(command: PushNotificationCommand): PushNotificationResult
}

data class PushNotificationCommand(
    val token: String,
    val title: String,
    val body: String,
    val deeplink: String?,
    val notificationId: Long,
)

data class PushNotificationResult(
    val status: PushNotificationSendStatus,
    val providerMessageId: String? = null,
    val failureReason: String? = null,
) {
    companion object {
        fun success(providerMessageId: String? = null): PushNotificationResult =
            PushNotificationResult(
                status = PushNotificationSendStatus.SUCCESS,
                providerMessageId = providerMessageId,
            )

        fun temporaryFailure(reason: String): PushNotificationResult =
            PushNotificationResult(
                status = PushNotificationSendStatus.TEMPORARY_FAILURE,
                failureReason = reason.take(200),
            )

        fun invalidToken(reason: String): PushNotificationResult =
            PushNotificationResult(
                status = PushNotificationSendStatus.INVALID_TOKEN,
                failureReason = reason.take(200),
            )

        fun skipped(reason: String): PushNotificationResult =
            PushNotificationResult(
                status = PushNotificationSendStatus.SKIPPED,
                failureReason = reason.take(200),
            )
    }
}

enum class PushNotificationSendStatus {
    SUCCESS,
    TEMPORARY_FAILURE,
    INVALID_TOKEN,
    SKIPPED,
}
