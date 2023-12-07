package no.nav.dagpenger.datadeling.teknisk

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.datadeling.AbstractApiTest
import no.nav.dagpenger.datadeling.api.installRetryClient
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HttpRetryClientTest : AbstractApiTest() {

    @Test
    fun `http-klienten prøver ikke retry om den får en 2xx-respons`() = testApplication {
        respondWith(HttpStatusCode.OK)

        var numberOfRequests = 0

        createRetryClient()
            .usingInterceptor { numberOfRequests += 1 }
            .get(PATH)

        assertEquals(1, numberOfRequests)
    }

    @Test
    fun `http-klienten prøver ikke retry om den får en 4xx-respons`() = testApplication {
        respondWith(HttpStatusCode.NotFound)

        var numberOfRequests = 0

        createRetryClient()
            .usingInterceptor { numberOfRequests += 1 }
            .get(PATH)

        assertEquals(1, numberOfRequests)
    }

    @Test
    fun `http-klienten prøver å nå ressurs på nytt dersom den får en 5xx-respons`() = testApplication {
        respondWith(HttpStatusCode.InternalServerError)

        val maxRetries = 5
        var numberOfRequests = 0

        createRetryClient(maxRetries)
            .usingInterceptor { numberOfRequests += 1 }
            .get(PATH)

        assertEquals(maxRetries + 1, numberOfRequests)
    }

    private fun ApplicationTestBuilder.createRetryClient(maxRetries: Int = 5) =
        createClient { installRetryClient(maksRetries = maxRetries, delayFunc = {}) }

    private fun HttpClient.usingInterceptor(block: (HttpRequestBuilder) -> Unit): HttpClient {
        plugin(HttpSend).intercept { request ->
            block(request)
            execute(request)
        }
        return this
    }

    private fun ApplicationTestBuilder.respondWith(statusCode: HttpStatusCode) {
        routing {
            get(PATH) {
                call.respond(statusCode)
            }
        }
    }

    private companion object {
        const val PATH = "/"
    }
}