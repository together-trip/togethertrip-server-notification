package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.domain.PushToken
import com.togethertrip.notification.notification.repository.PushTokenRepository
import com.togethertrip.notification.notification.service.command.RegisterPushTokenCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushTokenService(
    private val pushTokenRepository: PushTokenRepository,
) {

    @Transactional
    fun register(userId: Long, command: RegisterPushTokenCommand): PushToken {
        val tokenValue = command.token.trim()
        require(tokenValue.isNotBlank()) { "push token must not be blank" }

        val platform = command.platform
        val pushToken = pushTokenRepository.findByToken(tokenValue)
            ?.apply {
                register(
                    userId = userId,
                    platform = platform,
                    deviceId = command.deviceId?.takeIf { it.isNotBlank() },
                )
            }
            ?: pushTokenRepository.save(
                PushToken(
                    userId = userId,
                    token = tokenValue,
                    platform = platform,
                    deviceId = command.deviceId?.takeIf { it.isNotBlank() },
                ),
            )

        return pushToken
    }

    @Transactional
    fun deactivate(userId: Long, token: String) {
        val pushToken = pushTokenRepository.findByUserIdAndToken(userId, token.trim())
            ?: return
        pushToken.deactivate()
    }
}
