package no.nav.dagpenger.datadeling.testutil

import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.MaskinportenConfig
import no.nav.dagpenger.datadeling.RessursConfig
import no.nav.security.mock.oauth2.MockOAuth2Server

fun mockConfig(authServer: MockOAuth2Server = MockOAuth2Server()) =
    AppConfig(
        isLocal = true,
        maskinporten =
            MaskinportenConfig(
                discoveryUrl = authServer.wellKnownUrl("default").toString(),
                scope = "nav:dagpenger:vedtak.read",
                jwks_uri = authServer.jwksUrl("default").toUrl(),
                issuer = authServer.issuerUrl("default").toString(),
            ),
        ressurs =
            RessursConfig(
                minutterLevetidFerdig = 1L,
                minutterLevetidOpprettet = 1L,
                oppryddingsfrekvensMinutter = 1L,
            ),
    )
