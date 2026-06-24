package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.repository.NotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

@SpringBootTest
@ActiveProfiles("test")
@Import(CreateNotificationFromOutboxUseCase::class, ObjectMapperTestConfig::class)
@Transactional
class CreateNotificationFromOutboxUseCaseTest(
    @Autowired private val useCase: CreateNotificationFromOutboxUseCase,
    @Autowired private val notificationRepository: NotificationRepository,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `outbox 이벤트 recipients 기준으로 수신자별 알림을 생성한다`() {
        val message = sampleMessage(sourceEventId = 101L, recipientUserIds = listOf(1L, 2L, 3L))

        val result = useCase.create(message)

        assertEquals(3, result.createdCount)
        assertEquals(3, notificationRepository.count())
    }

    @Test
    fun `같은 sourceEventId와 수신자 조합은 중복 생성하지 않는다`() {
        val message = sampleMessage(sourceEventId = 102L, recipientUserIds = listOf(1L, 2L, 2L))

        val first = useCase.create(message)
        val second = useCase.create(message)

        assertEquals(2, first.createdCount)
        assertEquals(0, second.createdCount)
        assertEquals(2, notificationRepository.count())
    }

    private fun sampleMessage(sourceEventId: Long, recipientUserIds: List<Long>): MainOutboxEventMessage {
        val recipients = recipientUserIds.joinToString(",") { """{"userId":$it}""" }
        val payload = objectMapper.readTree(
            """
            {
              "recipients": [$recipients],
              "actorUserId": 99,
              "tripId": 10,
              "occurredAt": "2026-06-24T00:00:00Z",
              "eventVersion": 1
            }
            """.trimIndent(),
        )
        return MainOutboxEventMessage(
            id = sourceEventId,
            aggregateType = "TRIP",
            aggregateId = 10L,
            eventType = "TRIP_PARTICIPANTS_ADDED",
            payload = payload,
        )
    }
}

class ObjectMapperTestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
