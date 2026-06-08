package com.togethertrip.notification.global.logging

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class SensitiveDataMaskerTest {

    @Test
    fun `푸시 토큰과 payload를 마스킹한다`() {
        val masked = SensitiveDataMasker.mask("pushToken=abc123 payload=개인알림")

        assertFalse(masked.contains("abc123"))
        assertFalse(masked.contains("개인알림"))
        assertContains(masked, "***")
    }

    @Test
    fun `provider credential과 이메일을 마스킹한다`() {
        val masked = SensitiveDataMasker.mask("credential=secret user@example.com")

        assertFalse(masked.contains("secret"))
        assertFalse(masked.contains("user@example.com"))
    }
}
