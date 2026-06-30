package com.togethertrip.notification.notification.controller

import com.togethertrip.notification.global.response.ApiResponse
import com.togethertrip.notification.global.web.CurrentUserHeaders
import com.togethertrip.notification.notification.domain.PushTokenPlatform
import com.togethertrip.notification.notification.dto.request.DeletePushTokenRequest
import com.togethertrip.notification.notification.dto.request.RegisterPushTokenRequest
import com.togethertrip.notification.notification.dto.response.PushTokenResponse
import com.togethertrip.notification.notification.service.PushTokenService
import com.togethertrip.notification.notification.service.command.RegisterPushTokenCommand
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notification/api/push-tokens")
class PushTokenController(
    private val pushTokenService: PushTokenService,
) {

    @PostMapping
    fun register(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @RequestBody request: RegisterPushTokenRequest,
    ): ApiResponse<PushTokenResponse> =
        ApiResponse.success(
            PushTokenResponse.from(
                pushTokenService.register(userId, request.toCommand()),
            ),
        )

    @DeleteMapping
    fun deactivate(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @RequestBody request: DeletePushTokenRequest,
    ): ApiResponse<Unit> {
        pushTokenService.deactivate(userId, request.token)
        return ApiResponse.success()
    }

    private fun RegisterPushTokenRequest.toCommand(): RegisterPushTokenCommand =
        RegisterPushTokenCommand(
            token = token,
            platform = platform ?: PushTokenPlatform.UNKNOWN,
            deviceId = deviceId,
        )
}
