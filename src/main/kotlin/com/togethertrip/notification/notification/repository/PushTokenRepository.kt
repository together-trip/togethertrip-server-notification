package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.PushToken
import org.springframework.data.jpa.repository.JpaRepository

interface PushTokenRepository : JpaRepository<PushToken, Long> {

    fun findByToken(token: String): PushToken?

    fun findByUserIdAndToken(userId: Long, token: String): PushToken?

    fun findAllByUserIdAndActiveIsTrueAndDeletedAtIsNull(userId: Long): List<PushToken>
}
