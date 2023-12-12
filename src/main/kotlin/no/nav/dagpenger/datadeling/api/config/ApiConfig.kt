package no.nav.dagpenger.datadeling.api.config

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.dagpenger.datadeling.AppConfig
import org.slf4j.event.Level.INFO

fun Application.konfigurerApi(
    appMicrometerRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    appConfig: AppConfig
) {
    install(Authentication) {
        maskinporten(name = "afpPrivat", maskinportenConfig = appConfig.maskinporten)
    }

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(javaTimeModule)
        }
    }

    install(CallLogging) {
        disableDefaultColors()
        filter {
            it.request.path() !in setOf("/internal/prometheus", "/internal/liveness", "/internal/readyness")
        }
        level = INFO
    }
}