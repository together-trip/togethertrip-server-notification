package com.togethertrip.notification.notification.controller

import com.togethertrip.notification.global.web.CurrentUserHeaders
import com.togethertrip.notification.notification.dto.NotificationResponse
import com.togethertrip.notification.notification.service.NotificationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notification/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {

    @GetMapping
    fun getMyNotifications(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @RequestParam(defaultValue = "100") limit: Int,
    ): List<NotificationResponse> =
        notificationService.getMyNotifications(userId, limit)

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @PathVariable notificationId: Long,
    ): NotificationResponse =
        notificationService.markAsRead(userId, notificationId)

    @PatchMapping("/read-all")
    fun markAllAsRead(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
    ): MarkAllNotificationsReadResponse =
        MarkAllNotificationsReadResponse(
            updatedCount = notificationService.markAllAsRead(userId),
        )
}

data class MarkAllNotificationsReadResponse(
    val updatedCount: Int,
)
