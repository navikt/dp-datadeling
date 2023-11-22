package no.nav.dagpenger.datadeling.utils

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.application.*
import io.ktor.server.config.*
import no.nav.dagpenger.datadeling.AppConfig

inline fun Application.loadConfig(): AppConfig {

    return ConfigLoader.builder()
        .addKtorConfig(environment.config)
        .addEnvironmentSource()
        .build()
        .loadConfigOrThrow()
}

fun ConfigLoaderBuilder.addKtorConfig(config: ApplicationConfig) = apply {
    addPropertySource(MapPropertySource(config.toMap()))
}