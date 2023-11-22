package no.nav.dagpenger.datadeling.testutil

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.config.*
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.utils.addKtorConfig

inline fun loadConfig(config: ApplicationConfig): AppConfig {
    return ConfigLoader.builder()
        .addKtorConfig(config)
        .build()
        .loadConfigOrThrow()
}