package com.togethertrip.notification.notification.infrastructure.fcm

import com.togethertrip.notification.notification.push.PushNotificationSender
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpClient

@Configuration
@EnableConfigurationProperties(FcmPushProperties::class)
class FcmPushConfig {

    @Bean
    fun pushNotificationSender(
        properties: FcmPushProperties,
        objectMapper: ObjectMapper,
    ): PushNotificationSender {
        if (!properties.isConfigured()) {
            return NoopPushNotificationSender
        }

        return FcmHttpPushNotificationSender(
            properties = properties,
            accessTokenProvider = accessTokenProvider(properties, objectMapper),
            objectMapper = objectMapper,
            httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout)
                .build(),
        )
    }

    private fun accessTokenProvider(
        properties: FcmPushProperties,
        objectMapper: ObjectMapper,
    ): FcmAccessTokenProvider =
        when {
            properties.hasServiceAccount() -> ServiceAccountFcmAccessTokenProvider(properties, objectMapper)
            else -> StaticFcmAccessTokenProvider(properties.accessToken)
        }
}
