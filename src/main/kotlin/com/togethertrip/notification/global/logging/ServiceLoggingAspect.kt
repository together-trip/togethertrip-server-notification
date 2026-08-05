package com.togethertrip.notification.global.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Aspect
@Component
class ServiceLoggingAspect {

    @Around(
        "@within(org.springframework.stereotype.Service) || " +
            "execution(* com.togethertrip.notification.notification.push.PushNotificationSender+.send(..))",
    )
    fun logExecution(joinPoint: ProceedingJoinPoint): Any? {
        val startedAt = System.nanoTime()
        val method = joinPoint.signature.toShortString()
        val requestId = MDC.get(NotificationLoggingContext.REQUEST_ID) ?: NO_REQUEST_ID

        return try {
            joinPoint.proceed().also {
                log.info(
                    "notification method completed requestId={} method={} elapsedMs={}",
                    requestId,
                    method,
                    elapsedMillis(startedAt),
                )
            }
        } catch (failure: Throwable) {
            log.warn(
                "notification method failed requestId={} method={} elapsedMs={} exceptionType={}",
                requestId,
                method,
                elapsedMillis(startedAt),
                failure::class.simpleName ?: failure.javaClass.name,
            )
            throw failure
        }
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND

    companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000
        private const val NO_REQUEST_ID = "none"
        private val log = LoggerFactory.getLogger(ServiceLoggingAspect::class.java)
    }
}
