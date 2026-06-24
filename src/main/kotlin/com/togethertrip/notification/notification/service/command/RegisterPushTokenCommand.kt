package com.togethertrip.notification.notification.service.command

import com.togethertrip.notification.notification.domain.PushTokenPlatform

data class RegisterPushTokenCommand(
    val token: String,
    val platform: PushTokenPlatform,
    val deviceId: String?,
)
