package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.repository.NotificationRepository
import com.togethertrip.notification.notification.domain.DeletedAccount
import com.togethertrip.notification.notification.repository.DeletedAccountRepository
import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
@Import(CreateNotificationFromOutboxUseCase::class, ObjectMapperTestConfig::class)
@Transactional
class CreateNotificationFromOutboxUseCaseTest(
    @Autowired private val useCase: CreateNotificationFromOutboxUseCase,
    @Autowired private val notificationRepository: NotificationRepository,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val deletedAccountRepository: DeletedAccountRepository,
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

    @Test
    fun `trip recap 완료 이벤트는 모든 수신자에게 recap 알림을 생성한다`() {
        val payload = objectMapper.readTree(
            """
            {
              "eventVersion": 1,
              "tripId": 10,
              "tripRecapId": 100,
              "tripName": "제주 여행",
              "recipients": [
                { "userId": 1 },
                { "userId": 2 },
                { "userId": 2 }
              ],
              "occurredAt": "2026-07-06T12:00:00Z"
            }
            """.trimIndent(),
        )
        val message = MainOutboxEventMessage(
            id = 201L,
            aggregateType = "TRIP_RECAP",
            aggregateId = 100L,
            eventType = "TRIP_RECAP_COMPLETED",
            payload = payload,
        )

        val result = useCase.create(message)
        val notifications = notificationRepository.findAll().sortedBy { it.recipientUserId }

        assertEquals(2, result.createdCount)
        assertEquals(2, notificationRepository.count())
        assertEquals(listOf(1L, 2L), notifications.map { it.recipientUserId })
        assertEquals("지난 여행 Recap이 완성됐어요", notifications.first().title)
        assertEquals("제주 여행 추억을 확인해보세요.", notifications.first().body)
        assertEquals("togethertrip://trips/10/recap/100", notifications.first().deeplink)
    }

    @Test
    fun `tombstone 사용자는 지연된 알림 수신자에서 제외한다`() {
        deletedAccountRepository.save(
            DeletedAccount(
                userId = 2L,
                sourceEventId = 300L,
                deletedAt = Instant.parse("2026-07-28T12:00:00Z"),
            )
        )

        val result = useCase.create(
            sampleMessage(sourceEventId = 301L, recipientUserIds = listOf(1L, 2L, 3L))
        )

        assertEquals(2, result.createdCount)
        assertEquals(listOf(1L, 3L), notificationRepository.findAll().map { it.recipientUserId }.sorted())
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
