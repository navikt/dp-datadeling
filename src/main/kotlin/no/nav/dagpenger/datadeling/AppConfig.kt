package no.nav.dagpenger.datadeling

import java.net.URL

data class AppConfig(
    val isLocal: Boolean = false,
    val maskinporten: MaskinportenConfig,
    val ressurs: RessursConfig,
    val dpProxy: DpProxyConfig,
    val httpClient: HttpClientConfig,
    val db: DbConfig,
)

data class MaskinportenConfig(
    val discoveryurl: String,
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