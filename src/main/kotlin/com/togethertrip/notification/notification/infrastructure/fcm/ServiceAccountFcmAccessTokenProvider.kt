package com.togethertrip.notification.notification.infrastructure.fcm

import com.google.auth.oauth2.GoogleCredentials
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream

class ServiceAccountFcmAccessTokenProvider(
    properties: FcmPushProperties,
    objectMapper: ObjectMapper,
) : FcmAccessTokenProvider {

    private val credentials: GoogleCredentials = GoogleCredentials
        .fromStream(
            ByteArrayInputStream(
                objectMapper.writeValueAsBytes(properties.toServiceAccountJson()),
            ),
        )
        .createScoped(listOf(FCM_MESSAGING_SCOPE))

    @Synchronized
    override fun accessToken(): String {
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private fun FcmPushProperties.toServiceAccountJson(): Map<String, String> =
        mapOf(
            "type" to "service_account",
            "project_id" to projectId,
            "private_key_id" to privateKeyId,
            "private_key" to privateKey.replace("\\n", "\n"),
            "client_id" to clientId,
            "client_email" to clientEmail,
            "token_uri" to tokenUri.toString(),
        )

    private companion object {
        const val FCM_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    }
}
