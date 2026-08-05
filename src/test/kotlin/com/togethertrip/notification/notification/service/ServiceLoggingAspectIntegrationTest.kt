package com.togethertrip.notification.notification.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.togethertrip.notification.global.logging.ServiceLoggingAspect
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.stereotype.Service
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ServiceLoggingAspectIntegrationTest {

    @Test
    fun `Spring AOP 프록시는 결과를 보존하고 인자와 반환값을 기록하지 않는다`() {
        val appender = loggingAppender()
        val service = proxiedService()

        val result = service.succeed("push-token-secret")

        assertEquals("private-result", result)
        val event = appender.list.single()
        assertEquals(Level.INFO, event.level)
        assertTrue(event.formattedMessage.contains("requestId=none"))
        assertTrue(event.formattedMessage.contains("method="))
        assertTrue(event.formattedMessage.contains("elapsedMs="))
        assertFalse(event.formattedMessage.contains("push-token-secret"))
        assertFalse(event.formattedMessage.contains("private-result"))
    }

    @Test
    fun `Spring AOP 프록시는 예외를 다시 던지고 메시지를 기록하지 않는다`() {
        val appender = loggingAppender()
        val service = proxiedService()

        val thrown = assertFailsWith<IllegalArgumentException> {
            service.fail("payload-secret")
        }

        assertSame(SampleLoggingService.failure, thrown)
        val event = appender.list.single()
        assertEquals(Level.WARN, event.level)
        assertTrue(event.formattedMessage.contains("exceptionType=IllegalArgumentException"))
        assertFalse(event.formattedMessage.contains("payload-secret"))
        assertFalse(event.formattedMessage.contains("exception-message-secret"))
    }

    private fun proxiedService(): SampleServiceContract {
        val factory = AspectJProxyFactory(SampleLoggingService())
        factory.addAspect(ServiceLoggingAspect())
        return factory.getProxy()
    }

    private fun loggingAppender(): ListAppender<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(ServiceLoggingAspect::class.java) as Logger
        return ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
    }
}

private interface SampleServiceContract {
    fun succeed(input: String): String

    fun fail(input: String): String
}

@Service
private class SampleLoggingService : SampleServiceContract {
    override fun succeed(input: String): String = "private-result"

    override fun fail(input: String): String = throw failure

    companion object {
        val failure = IllegalArgumentException("exception-message-secret")
    }
}
