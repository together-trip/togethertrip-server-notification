package com.togethertrip.notification.notification.infrastructure.fcm

import com.togethertrip.notification.notification.push.PushNotificationCommand
import com.togethertrip.notification.notification.push.PushNotificationSendStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow

class FcmHttpPushNotificationSenderTest {

    private val properties = FcmPushProperties(
        enabled = true,
        projectId = "togethertrip-test",
        accessToken = "static-token",
    )
    private val accessTokenProvider = StaticFcmAccessTokenProvider("access-token")
    private val objectMapper = jacksonObjectMapper()
    private val httpClient = mock<HttpClient>()
    private val sender = FcmHttpPushNotificationSender(
        properties = properties,
        accessTokenProvider = accessTokenProvider,
        objectMapper = objectMapper,
        httpClient = httpClient,
    )

    @Test
    fun `FCM 요청은 HTTP v1 endpoint와 bearer token 그리고 알림 data를 포함한다`() {
        val response = mock<HttpResponse<String>>()
        whenever(response.statusCode()).thenReturn(200)
        whenever(response.body()).thenReturn("""{"name":"projects/togethertrip-test/messages/abc"}""")
        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(response)

        val result = sender.send(sampleCommand())

        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(httpClient).send(requestCaptor.capture(), any<HttpResponse.BodyHandler<String>>())
        val request = requestCaptor.firstValue
        val requestBody = request.bodyAsString()

        assertEquals(
            "https://fcm.googleapis.com/v1/projects/togethertrip-test/messages:send",
            request.uri().toString(),
        )
        assertEquals("Bearer access-token", request.headers().firstValue("Authorization").orElse(null))
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElse(null))
        assertTrue(requestBody.contains("device-token"))
        assertTrue(requestBody.contains("새 알림"))
        assertTrue(requestBody.contains("togethertrip://trips/10"))
        assertTrue(requestBody.contains("notificationId"))
        assertEquals(PushNotificationSendStatus.SUCCESS, result.status)
        assertEquals("projects/togethertrip-test/messages/abc", result.providerMessageId)
    }

    @Test
    fun `FCM invalid token 응답은 invalid token 결과로 분류한다`() {
        val response = mock<HttpResponse<String>>()
        whenever(response.statusCode()).thenReturn(404)
        whenever(response.body()).thenReturn("""{"error":{"status":"NOT_FOUND","message":"UNREGISTERED"}}""")
        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(response)

        val result = sender.send(sampleCommand())

        assertEquals(PushNotificationSendStatus.INVALID_TOKEN, result.status)
        assertEquals("fcm invalid token", result.failureReason)
    }

    @Test
    fun `FCM 인증 실패는 일시 실패로 분류한다`() {
        val response = mock<HttpResponse<String>>()
        whenever(response.statusCode()).thenReturn(401)
        whenever(response.body()).thenReturn("""{"error":"unauthorized"}""")
        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(response)

        val result = sender.send(sampleCommand())

        assertEquals(PushNotificationSendStatus.TEMPORARY_FAILURE, result.status)
        assertEquals("fcm credential rejected", result.failureReason)
    }

    private fun sampleCommand(): PushNotificationCommand =
        PushNotificationCommand(
            token = "device-token",
            title = "새 알림",
            body = "여행에 초대되었습니다.",
            deeplink = "togethertrip://trips/10",
            notificationId = 100L,
        )

    private fun HttpRequest.bodyAsString(): String {
        val publisher = bodyPublisher().orElseThrow()
        val result = CompletableFuture<ByteArray>()
        val chunks = mutableListOf<ByteArray>()

        publisher.subscribe(
            object : Flow.Subscriber<ByteBuffer> {
                private lateinit var subscription: Flow.Subscription

                override fun onSubscribe(subscription: Flow.Subscription) {
                    this.subscription = subscription
                    subscription.request(Long.MAX_VALUE)
                }

                override fun onNext(item: ByteBuffer) {
                    val bytes = ByteArray(item.remaining())
                    item.get(bytes)
                    chunks += bytes
                }

                override fun onError(throwable: Throwable) {
                    result.completeExceptionally(throwable)
                }

                override fun onComplete() {
                    result.complete(chunks.fold(ByteArray(0)) { acc, bytes -> acc + bytes })
                }
            },
        )

        return result.get().toString(StandardCharsets.UTF_8)
    }
}
