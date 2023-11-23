package no.nav.dagpenger.datadeling.testutil

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.config.*
import no.nav.dagpenger.datadeling.AppConfig

fun loadConfig(config: ApplicationConfig): AppConfig {
    return ConfigLoader.builder()
        .addPropertySource(MapPropertySource(config.toMap()))
        .build()
        .loadConfigOrThrow()
}