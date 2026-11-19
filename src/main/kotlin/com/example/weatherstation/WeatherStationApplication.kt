package com.example.weatherstation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication


@SpringBootApplication
@ConfigurationPropertiesScan
class WeatherStationApplication

fun main(args: Array<String>) {
    runApplication<WeatherStationApplication>(*args)
}
