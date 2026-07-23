package com.togethertrip.notification.notification.dto.request

import com.togethertrip.notification.notification.domain.PushTokenPlatform

data class RegisterPushTokenRequest(
    val token: String,
    val platform: PushTokenPlatform? = PushTokenPlatform.UNKNOWN,
    val deviceId: String? = null,
)
