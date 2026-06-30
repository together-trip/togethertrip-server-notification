package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.NotificationMessageQueue
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
@EnableConfigurationProperties(SqsNotificationConsumerProperties::class)
class SqsNotificationConsumerConfig {

    @Bean
    fun notificationMessageQueue(
        properties: SqsNotificationConsumerProperties,
    ): NotificationMessageQueue {
        if (!properties.enabled || !properties.hasQueueUrl()) {
            return NoopNotificationMessageQueue
        }

        val builder = SqsClient.builder()
            .region(Region.of(properties.region))

        if (properties.hasStaticCredentials()) {
            val credentials = if (properties.hasSessionToken()) {
                AwsSessionCredentials.create(
                    properties.accessKeyId,
                    properties.secretAccessKey,
                    properties.sessionToken,
                )
            } else {
                AwsBasicCredentials.create(
                    properties.accessKeyId,
                    properties.secretAccessKey,
                )
            }
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials))
        }

        val sqsClient = builder.build()

        return SqsNotificationMessageQueue(
            sqsClient = sqsClient,
            properties = properties,
        )
    }
}
