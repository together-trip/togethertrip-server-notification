package com.togethertrip.notification.notification.push

data class PushNotificationCommand(
    val token: String,
    val title: String,
    val body: String,
    val deeplink: String?,
    val notificationId: Long,
)
