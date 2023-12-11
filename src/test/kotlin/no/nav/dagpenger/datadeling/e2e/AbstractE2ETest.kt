package no.nav.dagpenger.datadeling.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.dagpenger.datadeling.Postgres
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

private const val MASKINPORTEN_ISSUER_ID = "maskinporten"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {

    private lateinit var testServerRuntime: TestServerRuntime

    private lateinit var proxyMockServer: WireMockServer
    private lateinit var mockOAuth2Server: MockOAuth2Server

    protected val client get() = testServerRuntime.restClient()

    @BeforeAll
    fun setupServer() {
        //sette opp alle properties som brukes
        val authServerPort = 8081

        mockOAuth2Server = MockOAuth2Server().also {
            it.start(authServerPort)
            System.setProperty("MASKINPORTEN_JWKS_URI", it.jwksUrl(MASKINPORTEN_ISSUER_ID).toString())
            System.setProperty("MASKINPORTEN_WELL_KNOWN_URL", "${it.wellKnownUrl(MASKINPORTEN_ISSUER_ID)}")
            System.setProperty("MASKINPORTEN_ISSUER", it.issuerUrl(MASKINPORTEN_ISSUER_ID).toString())
        }
        proxyMockServer = WireMockServer(8092).also {
            it.start()
            System.setProperty("DP_PROXY_URL", it.url("/"))
            System.setProperty("DP_PROXY_SCOPE", "nav:dagpenger:afpprivat.read")
        }
        Postgres.withMigratedDb()
        testServerRuntime = TestServer().start()
    }

    @AfterAll
    fun tearDownServer() {
        testServerRuntime.close()
    }

    val token
        get() = mockOAuth2Server.issueToken(
            issuerId = MASKINPORTEN_ISSUER_ID,
            clientId = "dp-datadeling",
            tokenCallback = DefaultOAuth2TokenCallback(claims = mapOf("scope" to "nav:dagpenger:afpprivat.read"))
        )

    fun mockProxyError(delayMs: Int = 0) {
        proxyMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/proxy/v1/arena/dagpengerperioder"))
                .willReturn(WireMock.serverError().withFixedDelay(delayMs))
        )
    }

    fun mockProxyResponse(response: DatadelingResponse, delayMs: Int = 0) {
        proxyMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/proxy/v1/arena/dagpengerperioder"))
                .willReturn(WireMock.jsonResponse(response, 200).withFixedDelay(delayMs))
        )
    }
}