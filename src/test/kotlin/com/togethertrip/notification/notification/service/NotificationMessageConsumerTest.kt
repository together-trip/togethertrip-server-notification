package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.service.message.ReceivedNotificationMessage
import com.togethertrip.notification.notification.service.result.CreateNotificationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper

class NotificationMessageConsumerTest {

    private val queue = FakeNotificationMessageQueue()
    private val useCase = mock<CreateNotificationFromOutboxUseCase>()
    private val consumer = NotificationMessageConsumer(
        notificationMessageQueue = queue,
        objectMapper = jacksonObjectMapper(),
        createNotificationFromOutboxUseCase = useCase,
    )

    @Test
    fun `메시지 처리 성공 시 queue 메시지를 acknowledge 한다`() {
        whenever(useCase.create(any())).thenReturn(CreateNotificationResult(createdCount = 2))
        val message = sampleMessage()

        consumer.handle(message)

        assertEquals(listOf(message), queue.acknowledgedMessages)
    }

    @Test
    fun `메시지 처리 실패 시 queue 메시지를 acknowledge 하지 않는다`() {
        whenever(useCase.create(any())).thenThrow(IllegalStateException("boom"))

        consumer.handle(sampleMessage())

        assertEquals(emptyList<ReceivedNotificationMessage>(), queue.acknowledgedMessages)
    }

    @Test
    fun `poll은 queue에서 받은 메시지를 use case로 전달한다`() {
        whenever(useCase.create(any())).thenReturn(CreateNotificationResult(createdCount = 1))
        queue.messages = listOf(sampleMessage())

        consumer.poll()

        verify(useCase).create(any())
    }

    @Test
    fun `no-op queue는 SQS 연결이 없어도 메시지를 반환하지 않는다`() {
        assertEquals(emptyList<ReceivedNotificationMessage>(), com.togethertrip.notification.notification.infrastructure.sqs.NoopNotificationMessageQueue.receive())
        verify(useCase, never()).create(any())
    }

    private fun sampleMessage(): ReceivedNotificationMessage =
        ReceivedNotificationMessage(
            id = "message-1",
            receiptHandle = "receipt-1",
            body = """
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
}

private class FakeNotificationMessageQueue : NotificationMessageQueue {
    var messages: List<ReceivedNotificationMessage> = emptyList()
    val acknowledgedMessages = mutableListOf<ReceivedNotificationMessage>()

    override fun receive(): List<ReceivedNotificationMessage> = messages

    override fun acknowledge(message: ReceivedNotificationMessage) {
        acknowledgedMessages += message
    }
}
