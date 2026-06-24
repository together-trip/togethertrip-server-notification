package com.togethertrip.notification.notification.service

interface NotificationMessageQueue {

    fun receive(): List<ReceivedNotificationMessage>

    fun acknowledge(message: ReceivedNotificationMessage)
}

data class ReceivedNotificationMessage(
    val id: String,
    val body: String,
    val receiptHandle: String,
)
