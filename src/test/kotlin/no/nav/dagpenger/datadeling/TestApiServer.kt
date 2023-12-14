package no.nav.dagpenger.datadeling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.Authentication
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.Routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.dagpenger.datadeling.TestApiServer.Companion.serverConfig
import no.nav.dagpenger.datadeling.api.config.maskinporten
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.slf4j.event.Level

class TestApiServer {
    companion object {
        private const val ISSUER = "default"
        private lateinit var mockOAuth2Server: MockOAuth2Server

        val serverConfig
            get() =
                MapApplicationConfig(
                    "port" to "9080",
                    "ENV" to "LOCAL",
                    "DP_PROXY_URL" to "http://0.0.0.0:8092/api",
                    "DP_PROXY_SCOPE" to "scope",
                    "DP_DATADELING_URL" to "http://localhost:8080",
                    @Suppress("standard:max-line-length")
                    "AZURE_APP_WELL_KNOWN_URL"
                        to "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration\"",
                    "AZURE_APP_CLIENT_ID" to "test",
                    "no.nav.security.jwt.issuers.0.issuer_name" to ISSUER,
                    "no.nav.security.jwt.issuers.0.discoveryurl" to mockOAuth2Server.wellKnownUrl(ISSUER).toString(),
                    "no.nav.security.jwt.issuers.0.accepted_audience" to "default",
                    "httpClient.retries" to "0",
                )
    }

    fun start() {
        mockOAuth2Server = MockOAuth2Server()
        mockOAuth2Server.start(8091)
    }

    fun shutdown() {
        mockOAuth2Server.shutdown()
    }

    fun createToken() =
        mockOAuth2Server.issueToken(
            issuerId = "default",
            clientId = "dp-datadeling",
            tokenCallback = DefaultOAuth2TokenCallback(claims = mapOf("scope" to "nav:dagpenger:vedtak.read")),
        )
}

fun ApplicationTestBuilder.testApiModule(
    appConfig: AppConfig,
    block: Routing.() -> Unit,
) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    install(Authentication) {
        maskinporten("afpPrivat", appConfig.maskinporten)
    }

    install(CallLogging) {
        disableDefaultColors()
        filter {
            it.request.path() !in setOf("/metrics", "/isalive", "/isready")
        }
        level = Level.INFO
    }

    environment {
        config = serverConfig
    }

    routing {
        block()
    }
}
