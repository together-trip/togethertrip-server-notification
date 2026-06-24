package com.togethertrip.notification.notification.infrastructure.sqs

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class SqsNotificationConsumerConfigTest {

    private val config = SqsNotificationConsumerConfig()

    @Test
    fun `SQS queue url이 없으면 no-op queue를 사용한다`() {
        val queue = config.notificationMessageQueue(
            properties = SqsNotificationConsumerProperties(queueUrl = ""),
        )

        assertSame(NoopNotificationMessageQueue, queue)
    }

    @Test
    fun `SQS queue url이 있으면 SQS queue를 사용한다`() {
        val queue = config.notificationMessageQueue(
            properties = SqsNotificationConsumerProperties(
                queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/togethertrip-prod-notification",
            ),
        )

        assertIs<SqsNotificationMessageQueue>(queue)
    }
}
