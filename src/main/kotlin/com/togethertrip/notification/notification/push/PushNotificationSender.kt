package com.togethertrip.notification.notification.push

interface PushNotificationSender {

    fun send(command: PushNotificationCommand): PushNotificationResult
}
