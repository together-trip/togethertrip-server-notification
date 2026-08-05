package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.PushToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PushTokenRepository : JpaRepository<PushToken, Long> {

    @Query("select token.id from PushToken token where token.userId = :userId")
    fun findIdsByUserId(@Param("userId") userId: Long): List<Long>

    @Modifying
    @Query("delete from PushToken token where token.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long): Int

    fun findByToken(token: String): PushToken?

    fun findByUserIdAndToken(userId: Long, token: String): PushToken?

    fun findAllByUserIdAndActiveIsTrueAndDeletedAtIsNull(userId: Long): List<PushToken>
}
