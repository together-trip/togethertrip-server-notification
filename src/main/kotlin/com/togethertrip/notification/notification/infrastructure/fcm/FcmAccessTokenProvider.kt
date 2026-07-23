package com.togethertrip.notification.notification.infrastructure.fcm

interface FcmAccessTokenProvider {

    fun accessToken(): String
}
