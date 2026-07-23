package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.service.message.ReceivedNotificationMessage

interface NotificationMessageQueue {

    fun receive(): List<ReceivedNotificationMessage>

    fun acknowledge(message: ReceivedNotificationMessage)
}
