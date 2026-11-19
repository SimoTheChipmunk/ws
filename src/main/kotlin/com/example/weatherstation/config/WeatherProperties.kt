package com.example.weatherstation.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties("weather")
@Validated
data class WeatherProperties(
    @Valid val ingestion: Ingestion = Ingestion(),
    @Valid val security: Security,
) {
    data class Ingestion(@Min(1) @Max(366) val maxRangeDays: Long = 366)
    data class Security(@NotBlank val writeApiKey: String)
}
