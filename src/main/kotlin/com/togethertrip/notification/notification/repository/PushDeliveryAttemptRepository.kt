package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.PushDeliveryAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PushDeliveryAttemptRepository : JpaRepository<PushDeliveryAttempt, Long> {
    @Modifying
    @Query("delete from PushDeliveryAttempt attempt where attempt.notificationId in :notificationIds")
    fun deleteAllByNotificationIds(@Param("notificationIds") notificationIds: Collection<Long>): Int

    @Modifying
    @Query("delete from PushDeliveryAttempt attempt where attempt.pushTokenId in :pushTokenIds")
    fun deleteAllByPushTokenIds(@Param("pushTokenIds") pushTokenIds: Collection<Long>): Int
}
