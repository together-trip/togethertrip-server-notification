package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.NotificationMessageQueue
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

        val sqsClient = SqsClient.builder()
            .region(Region.of(properties.region))
            .build()

        return SqsNotificationMessageQueue(
            sqsClient = sqsClient,
            properties = properties,
        )
    }
}
