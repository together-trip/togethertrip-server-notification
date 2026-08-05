package com.togethertrip.notification.global.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestLoggingFilterTest {

    private val filter = RequestLoggingFilter(RequestIdGenerator())

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `요청 ID를 생성해 응답과 처리 중 MDC에 제공하고 기존 MDC를 복원한다`() {
        MDC.put("upstream", "kept")
        val request = MockHttpServletRequest("GET", "/notification/api/notifications")
        val response = MockHttpServletResponse()
        var requestIdInChain: String? = null

        filter.doFilter(request, response, FilterChain { _, _ ->
            requestIdInChain = MDC.get(NotificationLoggingContext.REQUEST_ID)
        })

        assertNotNull(requestIdInChain)
        assertEquals(requestIdInChain, response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
        assertEquals("kept", MDC.get("upstream"))
        assertEquals(null, MDC.get(NotificationLoggingContext.REQUEST_ID))
    }

    @Test
    fun `안전한 기존 요청 ID와 사용자 ID를 MDC에 유지한다`() {
        val request = MockHttpServletRequest("GET", "/notification/api/notifications").apply {
            addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "gateway:request-123")
            addHeader("X-User-Id", "42")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ ->
            assertEquals("gateway:request-123", MDC.get(NotificationLoggingContext.REQUEST_ID))
            assertEquals("42", MDC.get(NotificationLoggingContext.USER_ID))
        })

        assertEquals("gateway:request-123", response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
    }

    @Test
    fun `로그 인젝션 가능성이 있는 요청 ID는 새 값으로 교체한다`() {
        val unsafeIds = listOf(
            "request 123",
            "request-123\nforged=true",
            "x".repeat(101),
        )

        unsafeIds.forEach { unsafeId ->
            val request = MockHttpServletRequest("GET", "/health").apply {
                addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, unsafeId)
            }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, FilterChain { _, _ -> })

            val generated = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)
            assertNotNull(generated)
            assertNotEquals(unsafeId, generated)
            assertTrue(Regex("[A-Za-z0-9._:-]{1,100}").matches(generated))
        }
    }

    @Test
    fun `실패 로그에 query 본문과 예외 메시지를 노출하지 않는다`() {
        val appender = loggingAppender()
        val request = MockHttpServletRequest("POST", "/notification/api/push-tokens").apply {
            queryString = "token=query-secret&email=user@example.com"
            setContent("{\"token\":\"body-secret\"}".toByteArray())
        }
        val response = MockHttpServletResponse()

        assertFailsWith<IllegalStateException> {
            filter.doFilter(request, response, FilterChain { _, _ ->
                throw IllegalStateException("exception-secret")
            })
        }

        val event = appender.list.single()
        assertEquals(Level.WARN, event.level)
        assertTrue(event.formattedMessage.contains("requestId="))
        assertTrue(event.formattedMessage.contains("path=/notification/api/push-tokens"))
        assertTrue(event.formattedMessage.contains("exceptionType=IllegalStateException"))
        listOf("query-secret", "user@example.com", "body-secret", "exception-secret")
            .forEach { secret -> assertEquals(false, event.formattedMessage.contains(secret)) }
    }

    private fun loggingAppender(): ListAppender<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java) as Logger
        return ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
    }
}
