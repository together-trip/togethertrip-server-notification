package com.togethertrip.notification.notification.controller

import com.togethertrip.notification.global.response.ApiResponse
import com.togethertrip.notification.global.web.CurrentUserHeaders
import com.togethertrip.notification.notification.dto.response.MarkAllNotificationsReadResponse
import com.togethertrip.notification.notification.dto.response.NotificationResponse
import com.togethertrip.notification.notification.dto.response.UnreadNotificationCountResponse
import com.togethertrip.notification.notification.service.NotificationService
import org.springframework.web.bind.annotation.DeleteMapping
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
    ): ApiResponse<List<NotificationResponse>> =
        ApiResponse.success(
            notificationService.getMyNotifications(userId, limit)
                .map(NotificationResponse::from),
        )

    @GetMapping("/unread-count")
    fun countUnreadNotifications(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
    ): ApiResponse<UnreadNotificationCountResponse> =
        ApiResponse.success(
            UnreadNotificationCountResponse(
                count = notificationService.countUnreadNotifications(userId),
            ),
        )

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @PathVariable notificationId: Long,
    ): ApiResponse<NotificationResponse> =
        ApiResponse.success(
            NotificationResponse.from(
                notificationService.markAsRead(userId, notificationId),
            ),
        )

    @PatchMapping("/read-all")
    fun markAllAsRead(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
    ): ApiResponse<MarkAllNotificationsReadResponse> =
        ApiResponse.success(
            MarkAllNotificationsReadResponse(
                updatedCount = notificationService.markAllAsRead(userId),
            ),
        )

    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @RequestHeader(CurrentUserHeaders.USER_ID) userId: Long,
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        notificationService.deleteNotification(userId, notificationId)
        return ApiResponse.success()
    }
}
