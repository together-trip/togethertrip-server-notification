package com.togethertrip.notification.notification.infrastructure.sqs

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "notification.sqs")
data class SqsNotificationConsumerProperties(
    val queueUrl: String = "",
    val region: String = "ap-northeast-2",
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val sessionToken: String = "",
    val enabled: Boolean = true,
    val maxMessages: Int = 10,
    val waitTime: Duration = Duration.ofSeconds(10),
    val visibilityTimeout: Duration = Duration.ofSeconds(30),
    val fixedDelay: Duration = Duration.ofSeconds(3),
) {
    fun hasQueueUrl(): Boolean = queueUrl.isNotBlank()

    fun hasStaticCredentials(): Boolean = accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()

    fun hasSessionToken(): Boolean = sessionToken.isNotBlank()
}
