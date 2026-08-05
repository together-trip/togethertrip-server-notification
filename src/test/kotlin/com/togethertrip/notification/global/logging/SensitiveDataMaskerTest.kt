package com.togethertrip.notification.global.logging

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveDataMaskerTest {

    @Test
    fun `secret key와 bearer token을 마스킹한다`() {
        val masked = SensitiveDataMasker.mask(
            "pushToken=push-secret&payload=private-body Authorization=Bearer bearer-secret",
        )

        listOf("push-secret", "private-body", "bearer-secret")
            .forEach { assertFalse(masked.contains(it)) }
        assertTrue(masked.contains("***"))
    }

    @Test
    fun `JSON 문자열과 개인정보를 마스킹한다`() {
        val masked = SensitiveDataMasker.mask(
            "{\"credential\":\"credential-secret\"} user@example.com 010-1234-5678",
        )

        assertEquals(
            "{\"credential\":\"***\"} *** ***",
            masked,
        )
    }
}
