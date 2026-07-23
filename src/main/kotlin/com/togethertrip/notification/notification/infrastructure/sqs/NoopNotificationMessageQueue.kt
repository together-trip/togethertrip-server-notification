package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.NotificationMessageQueue
import com.togethertrip.notification.notification.service.message.ReceivedNotificationMessage

object NoopNotificationMessageQueue : NotificationMessageQueue {

    override fun receive(): List<ReceivedNotificationMessage> = emptyList()

    override fun acknowledge(message: ReceivedNotificationMessage) = Unit
}
