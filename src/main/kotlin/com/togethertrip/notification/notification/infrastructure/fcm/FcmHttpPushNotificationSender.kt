package com.togethertrip.notification.notification.infrastructure.fcm

import com.togethertrip.notification.notification.push.PushNotificationCommand
import com.togethertrip.notification.notification.push.PushNotificationResult
import com.togethertrip.notification.notification.push.PushNotificationSender
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class FcmHttpPushNotificationSender(
    private val properties: FcmPushProperties,
    private val accessTokenProvider: FcmAccessTokenProvider,
    private val objectMapper: ObjectMapper,
    private val httpClient: HttpClient,
) : PushNotificationSender {

    override fun send(command: PushNotificationCommand): PushNotificationResult {
        val request = HttpRequest.newBuilder(endpointUri())
            .timeout(properties.timeout)
            .header("Authorization", "Bearer ${accessTokenProvider.accessToken()}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(command.toFcmRequest())))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        return when (response.statusCode()) {
            in 200..299 -> PushNotificationResult.success(providerMessageId = response.body().nameValue())
            400, 404 -> classifyClientFailure(response.body())
            401, 403 -> PushNotificationResult.temporaryFailure("fcm credential rejected")
            else -> PushNotificationResult.temporaryFailure("fcm status ${response.statusCode()}")
        }
    }

    private fun endpointUri(): URI =
        properties.endpoint.resolve("/v1/projects/${properties.projectId}/messages:send")

    private fun PushNotificationCommand.toFcmRequest(): Map<String, Any> {
        val data = buildMap {
            put("notificationId", notificationId.toString())
            deeplink?.let { put("deeplink", it) }
        }
        return mapOf(
            "message" to mapOf(
                "token" to token,
                "notification" to mapOf(
                    "title" to title,
                    "body" to body,
                ),
                "data" to data,
            ),
        )
    }

    private fun classifyClientFailure(body: String): PushNotificationResult {
        val normalizedBody = body.uppercase()
        return if (
            normalizedBody.contains("UNREGISTERED") ||
            normalizedBody.contains("INVALID_ARGUMENT") ||
            normalizedBody.contains("INVALID_REGISTRATION")
        ) {
            PushNotificationResult.invalidToken("fcm invalid token")
        } else {
            PushNotificationResult.temporaryFailure("fcm client failure")
        }
    }

    private fun String.nameValue(): String? {
        val node = runCatching { objectMapper.readTree(this) }.getOrNull() ?: return null
        return node.path("name").asStringOpt().orElse(null)
    }
}
