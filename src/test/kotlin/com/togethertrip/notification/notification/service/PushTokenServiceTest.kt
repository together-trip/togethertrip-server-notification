package com.togethertrip.notification.notification.service

import com.togethertrip.notification.global.exception.BusinessException
import com.togethertrip.notification.notification.domain.DeletedAccount
import com.togethertrip.notification.notification.domain.PushTokenPlatform
import com.togethertrip.notification.notification.exception.NotificationErrorCode
import com.togethertrip.notification.notification.repository.DeletedAccountRepository
import com.togethertrip.notification.notification.repository.PushTokenRepository
import com.togethertrip.notification.notification.service.command.RegisterPushTokenCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushTokenServiceTest(
    @Autowired private val pushTokenService: PushTokenService,
    @Autowired private val pushTokenRepository: PushTokenRepository,
    @Autowired private val deletedAccountRepository: DeletedAccountRepository,
) {

    @Test
    fun `같은 FCM token을 여러 번 등록해도 중복 저장하지 않는다`() {
        val command = RegisterPushTokenCommand(
            token = "fcm-token-1",
            platform = PushTokenPlatform.ANDROID,
            deviceId = "device-1",
        )

        pushTokenService.register(userId = 1L, command = command)
        pushTokenService.register(userId = 1L, command = command.copy(platform = PushTokenPlatform.IOS))

        val saved = pushTokenRepository.findByToken("fcm-token-1")
        assertEquals(1, pushTokenRepository.count())
        assertEquals(PushTokenPlatform.IOS, saved?.platform)
    }

    @Test
    fun `내 token만 비활성화한다`() {
        pushTokenService.register(
            userId = 1L,
            command = RegisterPushTokenCommand(
                token = "fcm-token-1",
                platform = PushTokenPlatform.UNKNOWN,
                deviceId = null,
            ),
        )
        pushTokenService.register(
            userId = 2L,
            command = RegisterPushTokenCommand(
                token = "fcm-token-2",
                platform = PushTokenPlatform.UNKNOWN,
                deviceId = null,
            ),
        )

        pushTokenService.deactivate(userId = 1L, token = "fcm-token-1")
        pushTokenService.deactivate(userId = 1L, token = "fcm-token-2")

        assertFalse(pushTokenRepository.findByToken("fcm-token-1")?.active ?: true)
        assertEquals(true, pushTokenRepository.findByToken("fcm-token-2")?.active)
    }

    @Test
    fun `삭제된 계정은 push token을 다시 등록할 수 없다`() {
        deletedAccountRepository.save(
            DeletedAccount(
                userId = 7L,
                sourceEventId = 301L,
                deletedAt = Instant.parse("2026-07-28T12:00:00Z"),
            )
        )

        val exception = assertFailsWith<BusinessException> {
            pushTokenService.register(
                userId = 7L,
                command = RegisterPushTokenCommand(
                    token = "deleted-user-token",
                    platform = PushTokenPlatform.IOS,
                    deviceId = "device-7",
                ),
            )
        }

        assertEquals(NotificationErrorCode.ACCOUNT_DELETED, exception.errorCode)
        assertEquals(0, pushTokenRepository.count())
    }
}
