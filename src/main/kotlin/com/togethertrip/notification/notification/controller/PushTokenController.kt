package com.togethertrip.notification.notification.controller

import com.togethertrip.notification.global.web.CurrentUserHeaders
import com.togethertrip.notification.notification.dto.DeletePushTokenRequest
import com.togethertrip.notification.notification.dto.PushTokenResponse
import com.togethertrip.notification.notification.dto.RegisterPushTokenRequest
import com.togethertrip.notification.notification.service.PushTokenService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
    ): PushTokenResponse =
        pushTokenService.register(userId, request)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @RequestBody request: DeletePushTokenRequest,
    ) {
        pushTokenService.deactivate(userId, request.token)
    }
}
