package no.nav.dagpenger.datadeling.e2e

import com.ctc.wstx.shaded.msv_core.datatype.xsd.IntType
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.dagpenger.datadeling.Postgres
import no.nav.dagpenger.datadeling.TestApplication
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

private const val MASKINPORTEN_ISSUER_ID = "maskinporten"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {

    private lateinit var testServerRuntime: TestServerRuntime
    private lateinit var proxyMockServer: WireMockServer
    protected val client get() = testServerRuntime.restClient()

    @BeforeAll
    fun setupServer() {
        //sette opp alle properties som brukes
        val authServerPort = 8081
        System.setProperty("DP_PROXY_CLIENT_MAX_RETRIES", "1")
        TestApplication.setup()

        proxyMockServer = WireMockServer(8092).also {
            it.start()
            System.setProperty("DP_PROXY_URL", it.url("/"))
            System.setProperty("DP_PROXY_SCOPE", "nav:dagpenger:afpprivat.read")
        }
        Postgres.withMigratedDb()
        testServerRuntime = TestServer().start()
    }

    @BeforeEach
    fun beforeEach() {
        proxyMockServer.resetAll()
    }

    @AfterAll
    fun tearDownServer() {
        testServerRuntime.close()
        proxyMockServer.shutdownServer()
        TestApplication.teardown()
        System.clearProperty("DP_PROXY_CLIENT_MAX_RETRIES")
        System.clearProperty("DP_PROXY_URL")
        System.clearProperty("DP_PROXY_SCOPE")
    }

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