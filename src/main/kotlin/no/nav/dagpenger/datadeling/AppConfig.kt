package no.nav.dagpenger.datadeling

import io.ktor.server.config.*

data class AppConfig(
    val dpProxyUrl: String,
    val dpProxyScope: String,
    val dpDatadelingUrl: String,
    val isLocal: Boolean,
    val maksRetries: Int,
) {
    companion object {
        fun fra(config: ApplicationConfig) = AppConfig(
            dpProxyUrl = config.property("DP_PROXY_URL").getString(),
            dpProxyScope = config.property("DP_PROXY_SCOPE").getString(),
            dpDatadelingUrl = config.property("DP_DATADELING_URL").getString(),
            isLocal = config.propertyOrNull("ENV")?.getString() == "LOCAL",
            maksRetries = config.property("httpClient.retries").getString().toInt(),
        )
    }
}