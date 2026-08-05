package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.Notification
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {

    @Query("select notification.id from Notification notification where notification.recipientUserId = :userId")
    fun findIdsByRecipientUserId(@Param("userId") userId: Long): List<Long>

    @Modifying
    @Query("delete from Notification notification where notification.recipientUserId = :userId")
    fun deleteAllByRecipientUserId(@Param("userId") userId: Long): Int

    @Query(
        """
        select n.recipientUserId
        from Notification n
        where n.sourceEventId = :sourceEventId
          and n.recipientUserId in :recipientUserIds
        """,
    )
    fun findExistingRecipientUserIds(
        @Param("sourceEventId") sourceEventId: Long,
        @Param("recipientUserIds") recipientUserIds: Collection<Long>,
    ): Set<Long>

    fun findByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        recipientUserId: Long,
        pageable: Pageable,
    ): List<Notification>

    fun findByIdAndRecipientUserIdAndDeletedAtIsNull(
        id: Long,
        recipientUserId: Long,
    ): Notification?

    fun countByRecipientUserIdAndReadAtIsNullAndDeletedAtIsNull(recipientUserId: Long): Long
}
