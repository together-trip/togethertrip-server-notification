package com.togethertrip.notification.notification.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NotificationMessageConsumerMetricsTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = MicrometerNotificationMessageConsumerMetrics(meterRegistry)

    @Test
    fun `계정 삭제 처리 결과를 outcome별 counter로 기록한다`() {
        metrics.recordAccountDeletion(AccountDeletionConsumeOutcome.DELETED)
        metrics.recordAccountDeletion(AccountDeletionConsumeOutcome.DUPLICATE)
        metrics.recordAccountDeletion(AccountDeletionConsumeOutcome.FAILED)
        metrics.recordAccountDeletion(AccountDeletionConsumeOutcome.FAILED)

        assertEquals(1.0, count("deleted"))
        assertEquals(1.0, count("duplicate"))
        assertEquals(2.0, count("failed"))
    }

    private fun count(outcome: String): Double = meterRegistry
        .get(MicrometerNotificationMessageConsumerMetrics.ACCOUNT_DELETION_COUNTER)
        .tag(MicrometerNotificationMessageConsumerMetrics.OUTCOME_TAG, outcome)
        .counter()
        .count()
}
