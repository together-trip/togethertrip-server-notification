package com.togethertrip.notification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
class NotificationApplicationTests @Autowired constructor(
    private val applicationContext: ApplicationContext,
) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `기본 보안 사용자를 자동 생성하지 않는다`() {
        assertTrue(applicationContext.getBeansOfType(UserDetailsService::class.java).isEmpty())
    }

}
