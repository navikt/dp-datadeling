package no.nav.dagpenger.datadeling

import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestApplication {
    private const val MASKINPORTEN_ISSUER_ID = "maskinporten"

    val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal fun issueMaskinportenToken(orgNummer: String = "orgNummer"): String {
        return mockOAuth2Server.issueToken(
            issuerId = MASKINPORTEN_ISSUER_ID,
            claims =
                mapOf(
                    "scope" to Config.appConfig.maskinporten.scope,
                    "consumer" to
                        mapOf(
                            "authority" to "NO",
                            "ID" to orgNummer,
                        ),
                ),
        ).serialize()
    }

    fun setup() {
        System.setProperty("MASKINPORTEN_JWKS_URI", mockOAuth2Server.jwksUrl(MASKINPORTEN_ISSUER_ID).toString())
        System.setProperty("MASKINPORTEN_WELL_KNOWN_URL", "${mockOAuth2Server.wellKnownUrl(MASKINPORTEN_ISSUER_ID)}")
        System.setProperty("MASKINPORTEN_ISSUER", mockOAuth2Server.issuerUrl(MASKINPORTEN_ISSUER_ID).toString())

        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "${mockOAuth2Server.tokenEndpointUrl("azureAd")}")
        System.setProperty("AZURE_APP_CLIENT_ID", "test")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "tull")
    }

    fun teardown() {
        System.clearProperty("MASKINPORTEN_JWKS_URI")
        System.clearProperty("MASKINPORTEN_WELL_KNOWN_URL")
        System.clearProperty("MASKINPORTEN_ISSUER")
        System.clearProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
        System.clearProperty("AZURE_APP_CLIENT_ID")
        System.clearProperty("AZURE_APP_CLIENT_SECRET")
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        return testApplication {
            setup()
            application(moduleFunction)
            test()
            teardown()
        }
    }
}
