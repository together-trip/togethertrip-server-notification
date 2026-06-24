package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.domain.PushTokenPlatform
import com.togethertrip.notification.notification.dto.request.RegisterPushTokenRequest
import com.togethertrip.notification.notification.dto.response.PushTokenResponse
import com.togethertrip.notification.notification.repository.PushTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushTokenService(
    private val pushTokenRepository: PushTokenRepository,
) {

    @Transactional
    fun register(userId: Long, request: RegisterPushTokenRequest): PushTokenResponse {
        val tokenValue = request.token.trim()
        require(tokenValue.isNotBlank()) { "push token must not be blank" }

        val platform = request.platform ?: PushTokenPlatform.UNKNOWN
        val pushToken = pushTokenRepository.findByToken(tokenValue)
            ?.apply {
                register(
                    userId = userId,
                    platform = platform,
                    deviceId = request.deviceId?.takeIf { it.isNotBlank() },
                )
            }
            ?: pushTokenRepository.save(
                PushToken(
                    userId = userId,
                    token = tokenValue,
                    platform = platform,
                    deviceId = request.deviceId?.takeIf { it.isNotBlank() },
                ),
            )

        return PushTokenResponse.from(pushToken)
    }

    @Transactional
    fun deactivate(userId: Long, token: String) {
        val pushToken = pushTokenRepository.findByUserIdAndToken(userId, token.trim())
            ?: return
        pushToken.deactivate()
    }
}
