package no.nav.dagpenger.datadeling.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalApiTest {

    @Test
    fun shouldGetAliveWithoutToken() = testApplication {
        routing { livenessRoutes(mockk()) }
        val response = client.get("/internal/liveness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alive", response.bodyAsText())
    }

    @Test
    fun shouldGetReadyWithoutToken() = testApplication {
        routing { livenessRoutes(mockk()) }
        val response = client.get("/internal/readyness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ready", response.bodyAsText())
    }

    @Test
    fun shouldGetMetricsWithoutToken() = testApplication {
        routing { livenessRoutes(mockk(relaxed = true)) }
        val response = client.get("/internal/prometheus")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
