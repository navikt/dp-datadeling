package no.nav.dagpenger.datadeling.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.dagpenger.datadeling.TestDatabase
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {
    private lateinit var testServerRuntime: TestServerRuntime
    private lateinit var testDatabase: TestDatabase

    private lateinit var proxyMockServer: WireMockServer
    private lateinit var mockOAuth2Server: MockOAuth2Server

    protected val client get() = testServerRuntime.restClient()
    protected val dataSource get() = testDatabase.dataSource

    @BeforeAll
    fun setupServer() {
        mockOAuth2Server = MockOAuth2Server().also { it.start() }
        testDatabase = TestDatabase()
        testServerRuntime = TestServer(testDatabase.dataSource).start()
        proxyMockServer = WireMockServer(8092).also { it.start() }
    }

    @AfterAll
    fun tearDownServer() {
        testServerRuntime.close()
    }

    @BeforeEach
    fun resetDatabase() {
        testDatabase.reset()
    }

    val token get() = mockOAuth2Server.issueToken("default", "dp-datadeling", DefaultOAuth2TokenCallback())

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