package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.ReceivedNotificationMessage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import kotlin.test.assertEquals

class SqsNotificationMessageQueueTest {

    private val sqsClient = mock<SqsClient>()
    private val queue = SqsNotificationMessageQueue(
        sqsClient = sqsClient,
        properties = SqsNotificationConsumerProperties(
            queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/togethertrip-prod-notification",
        ),
    )

    @Test
    fun `SQS 메시지를 queue message로 변환한다`() {
        whenever(sqsClient.receiveMessage(any<software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest>()))
            .thenReturn(
                ReceiveMessageResponse.builder()
                    .messages(
                        Message.builder()
                            .messageId("message-1")
                            .receiptHandle("receipt-1")
                            .body("{}")
                            .build(),
                    )
                    .build(),
            )

        val messages = queue.receive()

        assertEquals(listOf(ReceivedNotificationMessage("message-1", "{}", "receipt-1")), messages)
    }

    @Test
    fun `acknowledge는 SQS 메시지를 삭제한다`() {
        queue.acknowledge(ReceivedNotificationMessage("message-1", "{}", "receipt-1"))

        verify(sqsClient).deleteMessage(any<DeleteMessageRequest>())
    }
}
