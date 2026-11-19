package com.example.weatherstation.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

class WeatherPropertiesTest {

    private val contextRunner = ApplicationContextRunner().withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `valid properties bind successfully`() {
        contextRunner.withPropertyValues(
                "weather.security.write-api-key=test-write-key",
                "weather.ingestion.max-range-days=366",
            ).run { context ->
                assertTrue(context.isRunning)

                val properties = context.getBean(WeatherProperties::class.java)

                assertEquals("test-write-key", properties.security.writeApiKey)
                assertEquals(366L, properties.ingestion.maxRangeDays)
            }
    }

    @Test
    fun `blank write API key causes validation failure`() {
        contextRunner.withPropertyValues(
                "weather.security.write-api-key= ",
                "weather.ingestion.max-range-days=366",
            ).run { context ->
                assertNotNull(context.startupFailure)
            }
    }

    @Test
    fun `max range days below minimum causes validation failure`() {
        contextRunner.withPropertyValues(
                "weather.security.write-api-key=test-write-key",
                "weather.ingestion.max-range-days=0",
            ).run { context ->
                assertNotNull(context.startupFailure)
            }
    }

    @Test
    fun `weather properties do not expose provider selection`() {
        val propertyNames = WeatherProperties::class.java.declaredFields.map { it.name }

        assertTrue("provider" !in propertyNames)
        assertTrue("weatherCom" !in propertyNames)
    }


    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(WeatherProperties::class)
    class TestConfiguration {

        @Bean(name = ["configurationPropertiesValidator"])
        fun configurationPropertiesValidator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()
    }
}
