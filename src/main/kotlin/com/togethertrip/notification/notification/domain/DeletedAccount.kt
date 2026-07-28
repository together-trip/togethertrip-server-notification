package com.togethertrip.notification.notification.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "deleted_accounts",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_deleted_accounts_user", columnNames = ["user_id"]),
        UniqueConstraint(name = "uk_deleted_accounts_source_event", columnNames = ["source_event_id"]),
    ],
)
class DeletedAccount(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "source_event_id", nullable = false)
    val sourceEventId: Long,

    @Column(name = "deleted_at", nullable = false)
    val deletedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

