package no.nav.dagpenger.datadeling

import java.net.URL

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
