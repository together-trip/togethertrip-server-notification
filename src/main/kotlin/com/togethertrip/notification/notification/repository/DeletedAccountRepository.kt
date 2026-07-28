package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.DeletedAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DeletedAccountRepository : JpaRepository<DeletedAccount, Long> {
    fun existsByUserId(userId: Long): Boolean

    fun existsBySourceEventId(sourceEventId: Long): Boolean

    @Query("select account.userId from DeletedAccount account where account.userId in :userIds")
    fun findDeletedUserIds(@Param("userIds") userIds: Collection<Long>): Set<Long>

}
