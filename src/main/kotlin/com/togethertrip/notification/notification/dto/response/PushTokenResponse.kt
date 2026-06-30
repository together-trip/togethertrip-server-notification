package com.togethertrip.notification.notification.dto.response

import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.domain.PushTokenPlatform
import java.time.Instant

data class PushTokenResponse(
    val id: Long,
    val platform: PushTokenPlatform,
    val deviceId: String?,
    val active: Boolean,
    val lastRegisteredAt: Instant,
) {
    companion object {
        fun from(pushToken: PushToken): PushTokenResponse =
            PushTokenResponse(
                id = pushToken.id,
                platform = pushToken.platform,
                deviceId = pushToken.deviceId,
                active = pushToken.active,
                lastRegisteredAt = pushToken.lastRegisteredAt,
            )
    }
}
