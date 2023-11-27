package no.nav.dagpenger.datadeling.testutil

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.sources.MapPropertySource
import io.ktor.server.config.*
import no.nav.dagpenger.datadeling.*
import java.net.URL

inline fun loadConfig(config: ApplicationConfig): AppConfig {
    return ConfigLoader.builder()
        .addPropertySource(MapPropertySource(config.toMap()))
        .build()
        .loadConfigOrThrow()
}

val mockConfig = AppConfig (
    isLocal = true,
    maskinporten = MaskinportenConfig(
        discoveryurl = "",
        scope = "",
        jwks_uri = URL("http://localhost"),
        issuer = "",
    ),
    ressurs = RessursConfig(
        minutterLevetidFerdig = 1L,
        minutterLevetidOpprettet = 1L,
        oppryddingsfrekvensMinutter = 1L,
    ),
    dpProxy = DpProxyConfig(
        url = URL("http://localhost"),
        scope = "",
    ),
    httpClient = HttpClientConfig(
        retries = 0,
        host = URL("http://localhost"),
    ),
    db = DbConfig(
        host = "",
        port = 5432,
        name = "",
        username = "",
        password = "",
    ),
)