package com.togethertrip.notification.notification.domain

import com.togethertrip.notification.global.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notifications_source_event_recipient",
            columnNames = ["source_event_id", "recipient_user_id"],
        ),
    ],
)
class Notification(

    @Column(name = "source_event_id", nullable = false)
    val sourceEventId: Long,

    @Column(name = "recipient_user_id", nullable = false)
    val recipientUserId: Long,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @Column(name = "payload_snapshot", nullable = false, columnDefinition = "text")
    val payloadSnapshot: String,

    @Column(name = "occurred_at")
    val occurredAt: Instant? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null,
) : BaseEntity()
