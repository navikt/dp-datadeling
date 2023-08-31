package dp.datadeling.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import dp.datadeling.module
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class TestBase {

    companion object {
        const val ISSUER_ID = "default"
        const val REQUIRED_AUDIENCE = "default"

        var mockOAuth2Server = MockOAuth2Server()
        val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8092))

        @BeforeAll
        @JvmStatic
        fun setup() {
            mockOAuth2Server = MockOAuth2Server()
            mockOAuth2Server.start(8091)
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            mockOAuth2Server.shutdown()
            wireMockServer.stop()
        }
    }

    private fun setOidcConfig(): MapApplicationConfig {
        return MapApplicationConfig(
            "no.nav.security.jwt.issuers.size" to "1",
            "no.nav.security.jwt.issuers.0.issuer_name" to ISSUER_ID,
            "no.nav.security.jwt.issuers.0.discoveryurl" to mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString(),
            "no.nav.security.jwt.issuers.0.accepted_audience" to REQUIRED_AUDIENCE
        )
    }

    fun setUpTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        System.setProperty("DP_IVERKSETT_URL", "http://localhost:8092")
        System.setProperty("DP_IVERKSETT_SCOPE", "iverksett_scope")
        System.setProperty("DP_PROXY_URL", "http://localhost:8092/dp-proxy")
        System.setProperty("DP_PROXY_SCOPE", "proxy_scope")

        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "${mockOAuth2Server.tokenEndpointUrl("azureAd")}")
        System.setProperty("AZURE_APP_CLIENT_ID", "dp-proxy-12345")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "dp-proxy-secret")

        testApplication {
            environment {
                config = setOidcConfig()
            }
            application {
                module()
            }

            block()
        }
    }

    fun setUpMock(dpIverksettResponse: DatadelingResponse?, dpProxyResponse: DatadelingResponse?) {
        val dpIverksettHttpStatus = if (dpIverksettResponse == null) {
            HttpStatusCode.InternalServerError.value
        } else {
            HttpStatusCode.OK.value
        }
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/dagpengerperioder"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(dpIverksettHttpStatus)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(dpIverksettResponse))
                )
        )

        val dpProxyHttpStatus = if (dpProxyResponse == null) {
            HttpStatusCode.InternalServerError.value
        } else {
            HttpStatusCode.OK.value
        }
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/dagpengerperioder"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(dpProxyHttpStatus)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(dpProxyResponse))
                )
        )
    }
}
