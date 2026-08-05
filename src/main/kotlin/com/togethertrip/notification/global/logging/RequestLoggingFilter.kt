package com.togethertrip.notification.global.logging

import com.togethertrip.notification.global.web.CurrentUserHeaders
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter(
    private val requestIdGenerator: RequestIdGenerator,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val previousContext = MDC.getCopyOfContextMap()
        val requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER))
        val startedAt = System.nanoTime()
        var failureType: String? = null

        NotificationLoggingContext.putRequestId(requestId)
        NotificationLoggingContext.putUser(request.getHeader(CurrentUserHeaders.USER_ID))
        response.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(request, response)
        } catch (failure: Throwable) {
            failureType = failure::class.simpleName ?: failure.javaClass.name
            throw failure
        } finally {
            logCompletion(
                requestId = requestId,
                method = request.method,
                path = request.requestURI,
                status = response.status,
                elapsedMs = elapsedMillis(startedAt),
                failureType = failureType,
            )
            restoreMdc(previousContext)
        }
    }

    private fun resolveRequestId(candidate: String?): String =
        candidate
            ?.takeIf { REQUEST_ID_PATTERN.matches(it) }
            ?: requestIdGenerator.generate()

    private fun logCompletion(
        requestId: String,
        method: String,
        path: String,
        status: Int,
        elapsedMs: Long,
        failureType: String?,
    ) {
        if (failureType == null) {
            log.info(
                "notification http request completed requestId={} method={} path={} status={} elapsedMs={}",
                requestId,
                method,
                path,
                status,
                elapsedMs,
            )
            return
        }

        log.warn(
            "notification http request failed requestId={} method={} path={} status={} elapsedMs={} exceptionType={}",
            requestId,
            method,
            path,
            status,
            elapsedMs,
            failureType,
        )
    }

    private fun restoreMdc(previousContext: Map<String, String>?) {
        MDC.clear()
        previousContext?.let(MDC::setContextMap)
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val NANOS_PER_MILLISECOND = 1_000_000
        private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._:-]{1,100}")
    }
}
