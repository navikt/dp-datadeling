package no.nav.dagpenger.datadeling.api

import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.dagpenger.datadeling.AbstractApiTest
import no.nav.dagpenger.datadeling.testModule
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalApiTest : AbstractApiTest() {

    @Test
    fun shouldGetAliveWithoutToken() = testInternalApi {
        val response = client.get("/internal/liveness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alive", response.bodyAsText())
    }

    @Test
    fun shouldGetReadyWithoutToken() = testInternalApi {
        val response = client.get("/internal/readyness")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ready", response.bodyAsText())
    }

    @Test
    fun shouldGetMetricsWithoutToken() = testInternalApi {
        val response = client.get("/internal/prometheus")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    private fun testInternalApi(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            testModule(server.config) {
                apiRouting {
                    internalApi(mockk<PrometheusMeterRegistry>(relaxed = true))
                }
            }
            block()
        }
    }
}
