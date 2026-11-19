package com.example.weatherstation.support

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, TestProviderConfiguration::class)
@Tag("integration")
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var jdbc: JdbcClient

    @BeforeEach
    fun cleanDb() {
        jdbc.sql("DELETE FROM measurement").update(); jdbc.sql("DELETE FROM location").update()
    }
}
