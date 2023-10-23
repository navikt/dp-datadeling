package no.nav.dagpenger.datadeling.perioder

import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.datadeling.*
import no.nav.dagpenger.datadeling.AbstractApiTest
import no.nav.dagpenger.datadeling.ressurs.Ressurs
import no.nav.dagpenger.datadeling.ressurs.RessursService
import no.nav.dagpenger.datadeling.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class PerioderApiTest : AbstractApiTest() {
    private val ressursService: RessursService = mockk(relaxed = true)
    private val perioderService: PerioderService = mockk(relaxed = true)

    @Test
    fun `returnerer 401 uten token`() = testPerioderEndpoint {
        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), token = null).apply {
            assertEquals(HttpStatusCode.Unauthorized, this.status)
        }
    }

    @Test
    fun `returnerer 500 hvis oppretting av ressurs feiler`() = testPerioderEndpoint {
        coEvery { ressursService.opprett(any()) }.throws(Exception())

        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), server.createToken()).apply {
            assertEquals(HttpStatusCode.InternalServerError, this.status)
        }
    }

    @Test
    fun `returnerer 200 og url til ressurs`() = testPerioderEndpoint {
        val ressurs = enRessurs()
        coEvery { ressursService.opprett(any()) } returns ressurs
        coEvery { perioderService.hentDagpengeperioder(any()) } returns enDatadelingResponse()

        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), server.createToken())
            .apply { assertEquals(HttpStatusCode.Created, this.status) }
            .bodyAsText()
            .apply { assertEquals("http://localhost:8080/dagpenger/v1/periode/${ressurs.id}", this) }
    }

    @Test
    fun `returnerer ressurs om den finnes`() = testPerioderEndpoint {
        val id = 1L
        val response = enRessurs(
            id = id,
            status = RessursStatus.FERDIG,
            data = enDatadelingResponse(enPeriode(fraOgMed = 1.januar(), tilOgMed = null))
        )

        coEvery { ressursService.hent(id) } returns response

        client.testGet("/dagpenger/v1/periode/$id", token = server.createToken())
            .apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let {
                objectMapper.readValue(it.bodyAsText(), Ressurs::class.java)
            }
            .apply {
                assertEquals(1L, this.id)
                assertEquals(RessursStatus.FERDIG, this.status)
                assertEquals(response.data, this.data)
            }
    }

    private fun testPerioderEndpoint(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            testModule(server.config) {
                val appConfig = AppConfig.fra(environment!!.config)
                apiRouting {
                    perioderApi(
                        appConfig,
                        ressursService,
                        perioderService
                    )
                }
            }
            block()
        }
    }
}