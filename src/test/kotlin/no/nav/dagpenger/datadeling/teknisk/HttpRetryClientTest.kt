package no.nav.dagpenger.datadeling.teknisk

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.dagpenger.datadeling.AbstractApiTest
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
        createClient { installRetryClient(maxRetries = maxRetries, delayFunc = {}) }

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