package com.togethertrip.notification.notification.service

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

enum class AccountDeletionConsumeOutcome(val tagValue: String) {
    DELETED("deleted"),
    DUPLICATE("duplicate"),
    FAILED("failed"),
}

interface NotificationMessageConsumerMetrics {
    fun recordAccountDeletion(outcome: AccountDeletionConsumeOutcome)
}

@Component
class MicrometerNotificationMessageConsumerMetrics(
    private val meterRegistry: MeterRegistry,
) : NotificationMessageConsumerMetrics {
    override fun recordAccountDeletion(outcome: AccountDeletionConsumeOutcome) {
        meterRegistry.counter(
            ACCOUNT_DELETION_COUNTER,
            OUTCOME_TAG,
            outcome.tagValue,
        ).increment()
    }

    companion object {
        const val ACCOUNT_DELETION_COUNTER = "notification.account.deletion.consumed"
        const val OUTCOME_TAG = "outcome"
    }
}
