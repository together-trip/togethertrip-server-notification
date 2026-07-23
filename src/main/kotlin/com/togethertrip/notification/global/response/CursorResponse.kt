package com.togethertrip.notification.global.response

data class CursorResponse<T>(
    val items: List<T>,
    val nextCursor: Long?,
    val hasNext: Boolean,
    val size: Int,
)
