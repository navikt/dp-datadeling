package no.nav.dagpenger.datadeling.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.application.Application

fun Application.loadConfig(): AppConfig {
    return ConfigLoader.builder()
        .addPropertySource(MapPropertySource(environment.config.toMap()))
        .build()
        .loadConfigOrThrow()
}