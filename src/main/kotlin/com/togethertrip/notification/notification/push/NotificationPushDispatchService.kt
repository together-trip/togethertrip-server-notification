package com.togethertrip.notification.notification.push

import com.togethertrip.notification.notification.domain.Notification
import com.togethertrip.notification.notification.domain.PushDeliveryAttempt
import com.togethertrip.notification.notification.domain.PushDeliveryStatus
import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.repository.PushDeliveryAttemptRepository
import com.togethertrip.notification.notification.repository.PushTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPushDispatchService(
    private val pushTokenRepository: PushTokenRepository,
    private val pushDeliveryAttemptRepository: PushDeliveryAttemptRepository,
    private val pushNotificationSender: PushNotificationSender,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun dispatch(notification: Notification) {
        val pushTokens = pushTokenRepository.findAllByUserIdAndActiveIsTrueAndDeletedAtIsNull(
            userId = notification.recipientUserId,
        )
        if (pushTokens.isEmpty()) {
            return
        }

        pushTokens.forEach { pushToken ->
            val result = runCatching {
                pushNotificationSender.send(
                    PushNotificationCommand(
                        token = pushToken.token,
                        title = notification.title,
                        body = notification.body,
                        deeplink = notification.deeplink,
                        notificationId = notification.id,
                    ),
                )
            }.getOrElse { exception ->
                logger.warn(
                    "push notification send failed. notificationId={}, pushTokenId={}",
                    notification.id,
                    pushToken.id,
                    exception,
                )
                PushNotificationResult.temporaryFailure("sender exception")
            }

            handleResult(notification, pushToken, result)
        }
    }

    private fun handleResult(
        notification: Notification,
        pushToken: PushToken,
        result: PushNotificationResult,
    ) {
        if (result.status == PushNotificationSendStatus.INVALID_TOKEN) {
            pushToken.deactivate()
        }

        pushDeliveryAttemptRepository.save(
            PushDeliveryAttempt(
                notificationId = notification.id,
                pushTokenId = pushToken.id,
                status = result.status.toDeliveryStatus(),
                providerMessageId = result.providerMessageId,
                failureReason = result.failureReason,
            ),
        )
    }

    private fun PushNotificationSendStatus.toDeliveryStatus(): PushDeliveryStatus =
        when (this) {
            PushNotificationSendStatus.SUCCESS -> PushDeliveryStatus.SUCCESS
            PushNotificationSendStatus.TEMPORARY_FAILURE -> PushDeliveryStatus.TEMPORARY_FAILURE
            PushNotificationSendStatus.INVALID_TOKEN -> PushDeliveryStatus.INVALID_TOKEN
            PushNotificationSendStatus.SKIPPED -> PushDeliveryStatus.SKIPPED
        }
}
