package no.nav.dagpenger.datadeling.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.dagpenger.datadeling.Postgres
import no.nav.dagpenger.datadeling.TestApplication
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {
    private lateinit var proxyMockServer: WireMockServer

    @BeforeAll
    fun setupServer() {
        TestApplication.setup()

        proxyMockServer =
            WireMockServer(8092).also {
                it.start()
                System.setProperty("DP_PROXY_URL", it.url("/"))
                System.setProperty("DP_PROXY_SCOPE", "nav:dagpenger:afpprivat.read")
                System.setProperty("DP_MELDEKORTREGISTER_URL", it.url("/"))
                System.setProperty("DP_MELDEKORTREGISTER_SCOPE", "dev-gcp:teamdagpenger:dp-meldekortregister")
            }
        Postgres.withMigratedDb()
    }

    @BeforeEach
    fun beforeEach() {
        proxyMockServer.resetAll()
    }

    @AfterAll
    fun tearDownServer() {
        proxyMockServer.shutdownServer()
        TestApplication.teardown()
        System.clearProperty("DP_PROXY_URL")
        System.clearProperty("DP_PROXY_SCOPE")
        System.clearProperty("DP_MELDEKORTREGISTER_URL")
        System.clearProperty("DP_MELDEKORTREGISTER_SCOPE")
    }

    fun mockProxyError(delayMs: Int = 0) {
        proxyMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/proxy/v1/arena/dagpengerperioder"))
                .willReturn(WireMock.serverError().withFixedDelay(delayMs)),
        )
    }

    fun mockProxyResponse(
        response: DatadelingResponseDTO,
        delayMs: Int = 0,
    ) {
        proxyMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/proxy/v1/arena/dagpengerperioder"))
                .willReturn(WireMock.jsonResponse(response, 200).withFixedDelay(delayMs)),
        )
    }
}
