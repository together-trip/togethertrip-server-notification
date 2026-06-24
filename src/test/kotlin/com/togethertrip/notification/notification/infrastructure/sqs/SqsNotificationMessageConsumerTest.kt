package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.CreateNotificationFromOutboxUseCase
import com.togethertrip.notification.notification.service.CreateNotificationResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import tools.jackson.module.kotlin.jacksonObjectMapper

class SqsNotificationMessageConsumerTest {

    private val sqsClient = mock<SqsClient>()
    private val useCase = mock<CreateNotificationFromOutboxUseCase>()
    private val consumer = SqsNotificationMessageConsumer(
        sqsClient = sqsClient,
        properties = SqsNotificationConsumerProperties(
            queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/togethertrip-prod-notification",
        ),
        objectMapper = jacksonObjectMapper(),
        createNotificationFromOutboxUseCase = useCase,
    )

    @Test
    fun `메시지 처리 성공 시 SQS 메시지를 삭제한다`() {
        whenever(useCase.create(any())).thenReturn(CreateNotificationResult(createdCount = 2))

        consumer.handle(sampleSqsMessage())

        verify(sqsClient).deleteMessage(any<DeleteMessageRequest>())
    }

    @Test
    fun `메시지 처리 실패 시 SQS 메시지를 삭제하지 않는다`() {
        whenever(useCase.create(any())).thenThrow(IllegalStateException("boom"))

        consumer.handle(sampleSqsMessage())

        verify(sqsClient, never()).deleteMessage(any<DeleteMessageRequest>())
    }

    private fun sampleSqsMessage(): Message =
        Message.builder()
            .messageId("message-1")
            .receiptHandle("receipt-1")
            .body(
                """
                {
                  "id": 201,
                  "aggregateType": "TRIP",
                  "aggregateId": 10,
                  "eventType": "TRIP_PARTICIPANTS_ADDED",
                  "payload": {
                    "recipients": [
                      {"userId": 1},
                      {"userId": 2}
                    ],
                    "occurredAt": "2026-06-24T00:00:00Z",
                    "eventVersion": 1
                  }
                }
                """.trimIndent(),
            )
            .build()
}
