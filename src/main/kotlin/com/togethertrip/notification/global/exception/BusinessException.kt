package com.togethertrip.notification.global.exception

open class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
