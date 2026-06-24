package com.togethertrip.notification.notification.infrastructure.sqs

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
@EnableConfigurationProperties(SqsNotificationConsumerProperties::class)
class SqsNotificationConsumerConfig {

    @Bean
    @ConditionalOnMissingBean
    fun sqsClient(properties: SqsNotificationConsumerProperties): SqsClient =
        SqsClient.builder()
            .region(Region.of(properties.region))
            .build()
}
