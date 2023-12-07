package no.nav.dagpenger.datadeling.testutil

import no.nav.dagpenger.datadeling.config.AppConfig
import no.nav.dagpenger.datadeling.config.DbConfig
import no.nav.dagpenger.datadeling.config.DpProxyConfig
import no.nav.dagpenger.datadeling.config.HttpClientConfig
import no.nav.dagpenger.datadeling.config.MaskinportenConfig
import no.nav.dagpenger.datadeling.config.RessursConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.net.URL

fun mockConfig(serverPort: Int = 8080, authServer: MockOAuth2Server = MockOAuth2Server()) = AppConfig(
    isLocal = true,
    maskinporten = MaskinportenConfig(
        discoveryUrl = authServer.wellKnownUrl("default").toString(),
        scope = "nav:dagpenger:vedtak.read",
        jwks_uri = authServer.jwksUrl("default").toUrl(),
        issuer = authServer.issuerUrl("default").toString(),
    ),
    ressurs = RessursConfig(
        minutterLevetidFerdig = 1L,
        minutterLevetidOpprettet = 1L,
        oppryddingsfrekvensMinutter = 1L,
    ),
    dpProxy = DpProxyConfig(
        url = URL("http://localhost:8092"),
        scope = "",
    ),
    httpClient = HttpClientConfig(
        retries = 0,
        host = URL("http://localhost:$serverPort"),
    ),
    db = DbConfig(
        host = "",
        port = 5432,
        name = "",
        username = "",
        password = "",
    ),
)