package com.togethertrip.notification.notification.exception

import com.togethertrip.notification.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class NotificationErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
}
