package com.togethertrip.notification.notification.service

import org.springframework.context.annotation.Bean
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class ObjectMapperTestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
