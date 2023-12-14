package no.nav.dagpenger.datadeling.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalApiTest {
    @Test
    fun shouldGetAliveWithoutToken() =
        setupInternalApi {
            client.get("/internal/liveness").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Alive", response.bodyAsText())
            }
        }

    @Test
    fun shouldGetReadyWithoutToken() =
        setupInternalApi {
            client.get("/internal/readyness").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Ready", response.bodyAsText())
            }
        }

    @Test
    fun shouldGetMetricsWithoutToken() =
        setupInternalApi {
            assertEquals(HttpStatusCode.OK, client.get("/internal/prometheus").status)
        }

    private fun setupInternalApi(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            routing { livenessRoutes(mockk(relaxed = true)) }
            block(this)
        }
    }
}
