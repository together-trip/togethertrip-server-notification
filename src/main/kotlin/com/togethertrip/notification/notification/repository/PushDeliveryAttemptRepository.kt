package com.togethertrip.notification.notification.repository

import com.togethertrip.notification.notification.domain.PushDeliveryAttempt
import org.springframework.data.jpa.repository.JpaRepository

interface PushDeliveryAttemptRepository : JpaRepository<PushDeliveryAttempt, Long>
