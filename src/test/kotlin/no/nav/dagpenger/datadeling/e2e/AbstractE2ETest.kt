package no.nav.dagpenger.datadeling.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.testutil.mockConfig
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {
    private lateinit var testServerRuntime: TestServerRuntime

    private lateinit var proxyMockServer: WireMockServer
    private lateinit var mockOAuth2Server: MockOAuth2Server

    protected val client get() = testServerRuntime.restClient()
    protected lateinit var appConfig: AppConfig

    @BeforeAll
    fun setupServer() {
        val serverPort = ServerSocket(0).use { it.localPort }
        val authServerPort = 8081
        mockOAuth2Server = MockOAuth2Server().also { it.start(authServerPort) }
        appConfig = mockConfig(serverPort, mockOAuth2Server)

        testServerRuntime = TestServer(Config.datasource).start(appConfig, serverPort)
        proxyMockServer = WireMockServer(8092).also { it.start() }
    }

    @AfterAll
    fun tearDownServer() {
        testServerRuntime.close()
    }

    val token
        get() = mockOAuth2Server.issueToken(
            issuerId = "default",
            clientId = "dp-datadeling",
            tokenCallback = DefaultOAuth2TokenCallback(claims = mapOf("scope" to "nav:dagpenger:vedtak.read"))
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