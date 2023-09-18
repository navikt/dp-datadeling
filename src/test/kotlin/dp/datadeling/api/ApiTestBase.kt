package dp.datadeling.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.v2.tokenValidationSupport
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class ApiTestBase {
    companion object {
        private const val ISSUER = "default"
        private lateinit var mockOAuth2Server: MockOAuth2Server

        @JvmStatic
        @BeforeAll
        fun setup() {
            mockOAuth2Server = MockOAuth2Server()
            mockOAuth2Server.start(8091)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            mockOAuth2Server.shutdown()
        }
    }

    protected fun createToken() =
        mockOAuth2Server.issueToken(ISSUER, "someclientid", DefaultOAuth2TokenCallback())

    protected fun ApplicationTestBuilder.module(block: NormalOpenAPIRoute.() -> Unit) {
        val config = MapApplicationConfig(
            "no.nav.security.jwt.issuers.size" to "1",
            "no.nav.security.jwt.issuers.0.issuer_name" to ISSUER,
            "no.nav.security.jwt.issuers.0.discoveryurl" to mockOAuth2Server.wellKnownUrl(ISSUER).toString(),
            "no.nav.security.jwt.issuers.0.accepted_audience" to "default"
        )

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

}
