package com.example.weatherstation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.resilience.annotation.EnableResilientMethods
import java.time.Clock

@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class AppConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
