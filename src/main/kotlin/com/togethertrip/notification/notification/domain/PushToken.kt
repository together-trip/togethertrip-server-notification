package com.togethertrip.notification.notification.domain

import com.togethertrip.notification.global.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "push_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_push_tokens_token", columnNames = ["token"]),
    ],
)
class PushToken(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, length = 1024)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: PushTokenPlatform = PushTokenPlatform.UNKNOWN,

    @Column(name = "device_id", length = 120)
    var deviceId: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "last_registered_at", nullable = false)
    var lastRegisteredAt: Instant = Instant.now(),

    @Column(name = "invalidated_at")
    var invalidatedAt: Instant? = null,
) : BaseEntity() {

    fun register(
        userId: Long,
        platform: PushTokenPlatform,
        deviceId: String?,
        now: Instant = Instant.now(),
    ) {
        this.userId = userId
        this.platform = platform
        this.deviceId = deviceId
        active = true
        lastRegisteredAt = now
        invalidatedAt = null
        updatedAt = now
    }

    fun deactivate(now: Instant = Instant.now()) {
        active = false
        invalidatedAt = now
        updatedAt = now
    }
}
