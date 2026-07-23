package com.togethertrip.notification.notification.service.message

data class ReceivedNotificationMessage(
    val id: String,
    val body: String,
    val receiptHandle: String,
)
