package no.nav.dagpenger.datadeling

import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestApplication {
    private const val MASKINPORTEN_ISSUER_ID = "maskinporten"

    private val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal fun issueMaskinportenToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = MASKINPORTEN_ISSUER_ID,
            claims = mapOf(
                "scope" to Config.appConfig.maskinporten.scope,
            ),
        ).serialize()
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        System.setProperty("MASKINPORTEN_JWKS_URI", mockOAuth2Server.jwksUrl(MASKINPORTEN_ISSUER_ID).toString())
        System.setProperty("MASKINPORTEN_WELL_KNOWN_URL", "${mockOAuth2Server.wellKnownUrl(MASKINPORTEN_ISSUER_ID)}")
        System.setProperty("MASKINPORTEN_ISSUER", mockOAuth2Server.issuerUrl(MASKINPORTEN_ISSUER_ID).toString())

        return testApplication {
            application(moduleFunction)
            test()
        }
    }

    internal suspend fun ApplicationTestBuilder.autentisert(
        endepunkt: String,
        token: String = "token",
        httpMethod: HttpMethod = HttpMethod.Post,
        body: String? = null,
    ): HttpResponse {
        return client.request(endepunkt) {
            this.method = httpMethod
            body?.let { this.setBody(TextContent(it, ContentType.Application.Json)) }
            this.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
