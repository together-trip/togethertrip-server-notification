package com.togethertrip.notification.notification.domain

enum class PushDeliveryStatus {
    SUCCESS,
    TEMPORARY_FAILURE,
    INVALID_TOKEN,
    SKIPPED,
}
