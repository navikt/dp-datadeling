package no.nav.dagpenger.datadeling

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.application.Application
import java.net.URL

fun Application.loadConfig(): AppConfig {
    return ConfigLoader.builder()
        .addPropertySource(MapPropertySource(environment.config.toMap()))
        .build()
        .loadConfigOrThrow()
}

data class AppConfig(
    val isLocal: Boolean = false,
    val maskinporten: MaskinportenConfig,
    val ressurs: RessursConfig,
)

data class MaskinportenConfig(
    val discoveryUrl: String,
    val scope: String,
    val jwks_uri: URL,
    val issuer: String,
)

data class RessursConfig(
    val minutterLevetidOpprettet: Long,
    val minutterLevetidFerdig: Long,
    val oppryddingsfrekvensMinutter: Long,
)

data class DpProxyConfig(
    val url: URL,
    val scope: String,
)

data class HttpClientConfig(
    val retries: Int,
    val host: URL,
)

data class DbConfig(
    val host: String,
    val port: Int,
    val name: String,
    val username: String,
    val password: String,
)
