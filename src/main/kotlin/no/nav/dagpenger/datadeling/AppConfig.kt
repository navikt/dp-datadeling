package no.nav.dagpenger.datadeling

import io.ktor.server.config.*

data class AppConfig(
    val dpIverksettUrl: String,
    val dpIverksettScope: String,
    val dpProxyUrl: String,
    val dpProxyScope: String,
    val dpDatadelingUrl: String,
    val isLocal: Boolean,
) {
    companion object {
        fun fra(config: ApplicationConfig) = AppConfig(
            dpIverksettUrl = config.property("DP_IVERKSETT_URL").getString(),
            dpIverksettScope = config.property("DP_IVERKSETT_SCOPE").getString(),
            dpProxyUrl = config.property("DP_PROXY_URL").getString(),
            dpProxyScope = config.property("DP_PROXY_SCOPE").getString(),
            dpDatadelingUrl = config.property("DP_DATADELING_URL").getString(),
            isLocal = config.propertyOrNull("ENV")?.getString() == "LOCAL",
        )
    }
}