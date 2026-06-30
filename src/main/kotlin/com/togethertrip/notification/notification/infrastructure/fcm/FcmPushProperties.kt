package com.togethertrip.notification.notification.infrastructure.fcm

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(prefix = "notification.push.fcm")
data class FcmPushProperties(
    val enabled: Boolean = false,
    val projectId: String = "",
    val accessToken: String = "",
    val clientId: String = "",
    val clientEmail: String = "",
    val privateKey: String = "",
    val privateKeyId: String = "",
    val tokenUri: URI = URI.create("https://oauth2.googleapis.com/token"),
    val endpoint: URI = URI.create("https://fcm.googleapis.com"),
    val timeout: Duration = Duration.ofSeconds(3),
) {
    fun isConfigured(): Boolean =
        enabled && projectId.isNotBlank() && (hasStaticAccessToken() || hasServiceAccount())

    fun hasStaticAccessToken(): Boolean =
        accessToken.isNotBlank()

    fun hasServiceAccount(): Boolean =
        clientId.isNotBlank() &&
        clientEmail.isNotBlank() &&
            privateKey.isNotBlank() &&
            privateKeyId.isNotBlank()
}
