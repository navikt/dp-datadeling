package no.nav.dagpenger.datadeling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.v2.tokenValidationSupport

class TestApiServer {
    companion object {
        private const val ISSUER = "default"
        private lateinit var mockOAuth2Server: MockOAuth2Server
    }

    fun start() {
        mockOAuth2Server = MockOAuth2Server()
        mockOAuth2Server.start(8091)
    }

    fun shutdown() {
        mockOAuth2Server.shutdown()
    }

    fun createToken() =
        mockOAuth2Server.issueToken(ISSUER, "someclientid", DefaultOAuth2TokenCallback())

    val config
        get() = MapApplicationConfig(
            "ENV" to "LOCAL",
            "DP_PROXY_URL" to "http://0.0.0.0:8092/api",
            "DP_PROXY_SCOPE" to "scope",
            "DP_DATADELING_URL" to "http://localhost:8080",
            "AZURE_APP_WELL_KNOWN_URL" to "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration\"",
            "AZURE_APP_CLIENT_ID" to "test",
            "no.nav.security.jwt.issuers.size" to "1",
            "no.nav.security.jwt.issuers.0.issuer_name" to ISSUER,
            "no.nav.security.jwt.issuers.0.discoveryurl" to mockOAuth2Server.wellKnownUrl(ISSUER).toString(),
            "no.nav.security.jwt.issuers.0.accepted_audience" to "default",
            "httpClient.retries" to "0"
        )

}

fun ApplicationTestBuilder.testModule(config: ApplicationConfig, block: Routing.() -> Unit) {
    install(OpenAPIGen) {
        serveOpenApiJson = false
        serveSwaggerUi = false
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    environment {
        this.config = config
    }

    routing {
        apiRouting {
            block()
        }
    }
}