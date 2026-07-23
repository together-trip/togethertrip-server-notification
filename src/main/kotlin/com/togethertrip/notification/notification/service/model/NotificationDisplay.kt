package com.togethertrip.notification.notification.service.model

data class NotificationDisplay(
    val title: String,
    val body: String,
    val deeplink: String?,
)
