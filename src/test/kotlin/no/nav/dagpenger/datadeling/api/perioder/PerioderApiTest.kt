package no.nav.dagpenger.datadeling.api.perioder

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.TestApplication
import no.nav.dagpenger.datadeling.TestApplication.issueMaskinportenToken
import no.nav.dagpenger.datadeling.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.api.perioder.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testGet
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.datadeling.testutil.enPeriode
import no.nav.dagpenger.datadeling.testutil.enRessurs
import no.nav.dagpenger.datadeling.testutil.januar
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PerioderApiTest {
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
        Config.appConfig.maskinporten.also {
            println(it)
        }

        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), issueMaskinportenToken()).apply {
            assertEquals(HttpStatusCode.InternalServerError, this.status)
        }
    }

    @Test
    fun `returnerer 200 og url til ressurs`() = testPerioderEndpoint {
        val ressurs = enRessurs()
        coEvery { ressursService.opprett(any()) } returns ressurs
        coEvery { perioderService.hentDagpengeperioder(any()) } returns enDatadelingResponse()

        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), issueMaskinportenToken())
            .apply { assertEquals(HttpStatusCode.Created, this.status) }
            .bodyAsText()
            .apply { assertEquals("http://localhost:8080/dagpenger/v1/periode/${ressurs.uuid}", this) }
    }

    @Test
    fun `returnerer ressurs om den finnes`() = testPerioderEndpoint {
        val uuid = UUID.randomUUID()
        val response = enRessurs(
            uuid = uuid,
            status = RessursStatus.FERDIG,
            data = enDatadelingResponse(enPeriode(fraOgMed = 1.januar(), tilOgMed = null))
        )

        coEvery { ressursService.hent(uuid) } returns response

        client.testGet("/dagpenger/v1/periode/$uuid", token = issueMaskinportenToken())
            .apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let {
                objectMapper.readValue(it.bodyAsText(), Ressurs::class.java)
            }
            .apply {
                assertEquals(uuid, this.uuid)
                assertEquals(RessursStatus.FERDIG, this.status)
                assertEquals(response.response, this.response)
            }
    }

    private fun testPerioderEndpoint(block: suspend ApplicationTestBuilder.() -> Unit) {
        withMockAuthServerAndTestApplication(moduleFunction = {
            konfigurerApi(appConfig = Config.appConfig)
        }) {
            routing { perioderRoutes(ressursService, perioderService) }
            block()
        }
    }
}