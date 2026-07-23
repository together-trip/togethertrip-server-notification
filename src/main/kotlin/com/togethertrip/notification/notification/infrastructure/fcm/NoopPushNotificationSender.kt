package com.togethertrip.notification.notification.infrastructure.fcm

import com.togethertrip.notification.notification.push.PushNotificationCommand
import com.togethertrip.notification.notification.push.PushNotificationResult
import com.togethertrip.notification.notification.push.PushNotificationSender

object NoopPushNotificationSender : PushNotificationSender {

    override fun send(command: PushNotificationCommand): PushNotificationResult =
        PushNotificationResult.skipped("fcm is not configured")
}
