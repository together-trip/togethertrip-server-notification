package com.togethertrip.notification.notification.exception

import com.togethertrip.notification.global.exception.BusinessException

class NotificationNotFoundException(
    @Suppress("UNUSED_PARAMETER")
    notificationId: Long,
) : BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)
