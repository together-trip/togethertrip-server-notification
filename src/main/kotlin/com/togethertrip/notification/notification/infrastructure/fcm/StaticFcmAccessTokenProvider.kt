package com.togethertrip.notification.notification.infrastructure.fcm

class StaticFcmAccessTokenProvider(
    private val accessToken: String,
) : FcmAccessTokenProvider {

    override fun accessToken(): String = accessToken
}
