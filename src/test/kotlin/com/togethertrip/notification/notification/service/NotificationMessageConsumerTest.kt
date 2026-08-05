package com.togethertrip.notification.notification.service

import com.togethertrip.notification.global.logging.NotificationLoggingContext
import com.togethertrip.notification.notification.service.message.ReceivedNotificationMessage
import com.togethertrip.notification.notification.service.result.CreateNotificationResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper

class NotificationMessageConsumerTest {

    private val queue = FakeNotificationMessageQueue()
    private val useCase = mock<CreateNotificationFromOutboxUseCase>()
    private val accountDeletionService = mock<AccountDeletionService>()
    private val metrics = RecordingNotificationMessageConsumerMetrics()
    private val consumer = NotificationMessageConsumer(
        notificationMessageQueue = queue,
        objectMapper = jacksonObjectMapper(),
        createNotificationFromOutboxUseCase = useCase,
        accountDeletionService = accountDeletionService,
        metrics = metrics,
    )

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

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
    fun `outbox event ID를 처리 중 correlation requestId로 사용하고 기존 MDC를 복원한다`() {
        MDC.put(NotificationLoggingContext.REQUEST_ID, "upstream-request")
        MDC.put(NotificationLoggingContext.EVENT_TYPE, "UPSTREAM_EVENT")
        whenever(useCase.create(any())).thenAnswer {
            assertEquals("outbox:201", NotificationLoggingContext.currentRequestId())
            assertEquals(
                "TRIP_PARTICIPANTS_ADDED",
                MDC.get(NotificationLoggingContext.EVENT_TYPE),
            )
            CreateNotificationResult(createdCount = 1)
        }

        consumer.handle(sampleMessage())

        assertEquals("upstream-request", NotificationLoggingContext.currentRequestId())
        assertEquals("UPSTREAM_EVENT", MDC.get(NotificationLoggingContext.EVENT_TYPE))
    }

    @Test
    fun `계정 삭제 이벤트는 전용 service로 전달하고 성공 시 acknowledge 한다`() {
        whenever(accountDeletionService.delete(any())).thenReturn(true)
        val message = accountDeletionMessage()

        consumer.handle(message)

        verify(accountDeletionService).delete(any())
        verify(useCase, never()).create(any())
        assertEquals(listOf(message), queue.acknowledgedMessages)
        assertEquals(listOf(AccountDeletionConsumeOutcome.DELETED), metrics.accountDeletionOutcomes)
    }

    @Test
    fun `중복 계정 삭제 이벤트도 acknowledge하고 duplicate metric을 기록한다`() {
        whenever(accountDeletionService.delete(any())).thenReturn(false)
        val message = accountDeletionMessage()

        consumer.handle(message)

        assertEquals(listOf(message), queue.acknowledgedMessages)
        assertEquals(listOf(AccountDeletionConsumeOutcome.DUPLICATE), metrics.accountDeletionOutcomes)
    }

    @Test
    fun `잘못된 계정 삭제 payload 처리 실패는 acknowledge 하지 않는다`() {
        whenever(accountDeletionService.delete(any())).thenThrow(IllegalArgumentException("invalid payload"))

        consumer.handle(accountDeletionMessage())

        assertEquals(emptyList<ReceivedNotificationMessage>(), queue.acknowledgedMessages)
        assertEquals(listOf(AccountDeletionConsumeOutcome.FAILED), metrics.accountDeletionOutcomes)
    }

    @Test
    fun `계정 삭제 acknowledge 실패는 deleted 대신 failed metric만 기록한다`() {
        whenever(accountDeletionService.delete(any())).thenReturn(true)
        queue.acknowledgeFailure = IllegalStateException("ack failed")

        consumer.handle(accountDeletionMessage())

        assertEquals(listOf(AccountDeletionConsumeOutcome.FAILED), metrics.accountDeletionOutcomes)
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

    private fun accountDeletionMessage(): ReceivedNotificationMessage =
        ReceivedNotificationMessage(
            id = "message-account-deleted",
            receiptHandle = "receipt-account-deleted",
            body = """
                {
                  "id": 301,
                  "aggregateType": "USER",
                  "aggregateId": 7,
                  "eventType": "USER_ACCOUNT_DELETED",
                  "payload": {
                    "eventVersion": 1,
                    "userId": 7,
                    "occurredAt": "2026-07-28T12:00:00Z"
                  }
                }
            """.trimIndent(),
        )
}

private class RecordingNotificationMessageConsumerMetrics : NotificationMessageConsumerMetrics {
    val accountDeletionOutcomes = mutableListOf<AccountDeletionConsumeOutcome>()

    override fun recordAccountDeletion(outcome: AccountDeletionConsumeOutcome) {
        accountDeletionOutcomes += outcome
    }
}

private class FakeNotificationMessageQueue : NotificationMessageQueue {
    var messages: List<ReceivedNotificationMessage> = emptyList()
    val acknowledgedMessages = mutableListOf<ReceivedNotificationMessage>()
    var acknowledgeFailure: Exception? = null

    override fun receive(): List<ReceivedNotificationMessage> = messages

    override fun acknowledge(message: ReceivedNotificationMessage) {
        acknowledgeFailure?.let { throw it }
        acknowledgedMessages += message
    }
}
