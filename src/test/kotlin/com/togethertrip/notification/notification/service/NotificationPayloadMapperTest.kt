package com.togethertrip.notification.notification.service

import com.togethertrip.notification.notification.service.message.MainOutboxEventMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class NotificationPayloadMapperTest {

    private val objectMapper = jacksonObjectMapper()
    private val mapper = NotificationPayloadMapper()

    @Test
    fun `게시글 생성 이벤트를 title body deeplink로 매핑한다`() {
        val message = MainOutboxEventMessage(
            id = 1L,
            aggregateType = "POST",
            aggregateId = 20L,
            eventType = "POST_CREATED",
            payload = objectMapper.readTree(
                """
                {
                  "tripId": 10,
                  "postId": 20,
                  "tripName": "제주 여행",
                  "actorDisplayName": "지우",
                  "title": "첫 일정"
                }
                """.trimIndent(),
            ),
        )

        val display = mapper.map(message)

        assertEquals("제주 여행", display.title)
        assertEquals("지우님이 첫 일정을 작성했습니다.", display.body)
        assertEquals("togethertrip://trips/10/posts/20", display.deeplink)
    }

    @Test
    fun `알 수 없는 이벤트는 안전한 fallback 문구로 매핑한다`() {
        val message = MainOutboxEventMessage(
            id = 1L,
            aggregateType = "UNKNOWN",
            aggregateId = 20L,
            eventType = "UNKNOWN_EVENT",
            payload = objectMapper.readTree("""{"recipients":[{"userId":1}]}"""),
        )

        val display = mapper.map(message)

        assertEquals("새 알림이 도착했습니다", display.title)
        assertEquals("UNKNOWN UNKNOWN_EVENT 이벤트가 도착했습니다.", display.body)
        assertEquals(null, display.deeplink)
    }
}
