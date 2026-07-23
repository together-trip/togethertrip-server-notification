package com.togethertrip.notification.global.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
                message = null,
            )

        fun success(): ApiResponse<Unit> =
            ApiResponse(
                success = true,
                data = Unit,
                message = null,
            )

        fun <T> success(data: T, message: String): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
                message = message,
            )
    }
}
