package com.togethertrip.notification.notification.domain

import com.togethertrip.notification.global.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "push_delivery_attempts")
class PushDeliveryAttempt(

    @Column(name = "notification_id", nullable = false)
    val notificationId: Long,

    @Column(name = "push_token_id", nullable = false)
    val pushTokenId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: PushDeliveryStatus,

    @Column(name = "provider_message_id", length = 200)
    val providerMessageId: String? = null,

    @Column(name = "failure_reason", length = 200)
    val failureReason: String? = null,

    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: Instant = Instant.now(),
) : BaseEntity()
