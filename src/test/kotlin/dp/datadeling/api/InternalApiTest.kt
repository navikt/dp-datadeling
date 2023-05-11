package dp.datadeling.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalApiTest : TestBase() {

    @Test
    fun shouldGetAliveWithoutToken() = setUpTestApplication {
        val response = client.get("/internal/liveness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alive", response.bodyAsText())
    }

    @Test
    fun shouldGetReadyWithoutToken() = setUpTestApplication {
        val response = client.get("/internal/readyness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ready", response.bodyAsText())
    }

    @Test
    fun shouldGetMetricsWithoutToken() = setUpTestApplication {
        val response = client.get("/internal/prometheus")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
