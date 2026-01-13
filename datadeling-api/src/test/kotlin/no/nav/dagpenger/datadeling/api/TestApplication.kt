package no.nav.dagpenger.datadeling.api

import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.datadeling.sporing.NoopLogger
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestApplication {
    private const val MASKINPORTEN_ISSUER_ID = "maskinporten"
    private const val AZURE_ISSUER_ID = "azure"
    private const val AZURE_APP_CLIENT_ID = "test"

    val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal fun issueMaskinportenToken(orgNummer: String = "0192:889640782"): String =
        mockOAuth2Server
            .issueToken(
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

    internal fun issueAzureToken(
        azpName: String? = null,
        azpId: String? = null,
        azpRoles: List<String> = emptyList(),
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = AZURE_ISSUER_ID,
                audience = AZURE_APP_CLIENT_ID,
                claims =
                    listOfNotNull(
                        azpName?.let { "azp_name" to it },
                        azpId?.let { "azp" to it },
                        azpRoles.let { "roles" to it },
                    ).toMap(),
            ).serialize()

    fun setup() {
        System.setProperty("MASKINPORTEN_JWKS_URI", mockOAuth2Server.jwksUrl(MASKINPORTEN_ISSUER_ID).toString())
        System.setProperty("MASKINPORTEN_WELL_KNOWN_URL", mockOAuth2Server.wellKnownUrl(MASKINPORTEN_ISSUER_ID).toString())
        System.setProperty("MASKINPORTEN_ISSUER", mockOAuth2Server.issuerUrl(MASKINPORTEN_ISSUER_ID).toString())

        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "${mockOAuth2Server.tokenEndpointUrl(AZURE_ISSUER_ID)}")
        System.setProperty("AZURE_APP_CLIENT_ID", AZURE_APP_CLIENT_ID)
        System.setProperty("AZURE_APP_CLIENT_SECRET", "tull")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", mockOAuth2Server.jwksUrl(AZURE_ISSUER_ID).toString())
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", mockOAuth2Server.wellKnownUrl(AZURE_ISSUER_ID).toString())
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", mockOAuth2Server.issuerUrl(AZURE_ISSUER_ID).toString())
    }

    fun teardown() {
        System.clearProperty("MASKINPORTEN_JWKS_URI")
        System.clearProperty("MASKINPORTEN_WELL_KNOWN_URL")
        System.clearProperty("MASKINPORTEN_ISSUER")
        System.clearProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
        System.clearProperty("AZURE_APP_CLIENT_ID")
        System.clearProperty("AZURE_APP_CLIENT_SECRET")
        System.clearProperty("AZURE_APP_WELL_KNOWN_URL")
        System.clearProperty("AZURE_OPENID_CONFIG_JWKS_URI")
        System.clearProperty("AZURE_OPENID_CONFIG_ISSUER")
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend TestContext.() -> Unit,
    ) {
        try {
            setup()
            return naisfulTestApp(
                {
                    apply { moduleFunction() }
                },
                objectMapper,
                PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            ) {
                test()
            }
        } finally {
            teardown()
        }
    }

    internal fun testEndepunkter(
        perioderService: PerioderService = mockk(relaxed = true),
        meldekortService: MeldekortService = mockk(relaxed = true),
        ressursService: RessursService = mockk(relaxed = true),
        behandlingRepository: BehandlingResultatRepositoryMedTolker = mockk(relaxed = true),
        auditLogger: Log = NoopLogger,
        test: suspend TestContext.() -> Unit,
    ) {
        withMockAuthServerAndTestApplication(moduleFunction = {
            datadelingApi(
                logger = auditLogger,
                config = Config.appConfig,
                perioderService = perioderService,
                meldekortService = meldekortService,
                ressursService = ressursService,
                behandlingRepository = behandlingRepository,
            )
        }) {
            test()
        }
    }
}
