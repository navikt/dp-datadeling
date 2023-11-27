package no.nav.dagpenger.datadeling.utils

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.application.*
import no.nav.dagpenger.datadeling.AppConfig

inline fun Application.loadConfig(): AppConfig {

    return ConfigLoader.builder()
        .addPropertySource(MapPropertySource(environment.config.toMap()))
        .build()
        .loadConfigOrThrow()
}