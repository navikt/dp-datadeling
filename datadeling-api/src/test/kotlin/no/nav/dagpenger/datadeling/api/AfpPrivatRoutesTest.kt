package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.TestApplication.issueMaskinportenToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.models.DatadelingResponseAfpDTO
import no.nav.dagpenger.datadeling.models.PeriodeAfpDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.datadeling.testGet
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.datadeling.testutil.enRessurs
import no.nav.dagpenger.dato.januar
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AfpPrivatRoutesTest {
    private val ressursService: RessursService =
        mockk<RessursService>(relaxed = true)
    private val perioderService: PerioderService = mockk(relaxed = true)

    @Test
    fun `returnerer 401 uten token`() =
        testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
            client.testPost("/dagpenger/datadeling/v1/periode", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 500 hvis oppretting av ressurs feiler`() =
        testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
            coEvery { ressursService.opprett(any()) }.throws(Exception())
            Config.appConfig.maskinporten.also {
                println(it)
            }

            client.testPost("/dagpenger/datadeling/v1/periode", enDatadelingRequest(), issueMaskinportenToken()).apply {
                assertEquals(HttpStatusCode.InternalServerError, this.status)
            }
        }

    @Test
    fun `returnerer 200 og url til ressurs`() =
        testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
            val ressurs = enRessurs()
            coEvery { ressursService.opprett(any()) } returns ressurs
            coEvery { perioderService.hentDagpengeperioder(any()) } returns enDatadelingResponse()

            client
                .testPost("/dagpenger/datadeling/v1/periode", enDatadelingRequest(), issueMaskinportenToken())
                .apply { assertEquals(HttpStatusCode.Created, this.status) }
                .bodyAsText()
                .apply { assertEquals("http://localhost/dagpenger/datadeling/v1/periode/${ressurs.uuid}", this) }
        }

    @Test
    fun `returnerer 404 om en ressus ikke finnes`() {
        testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
            val uuid = UUID.randomUUID()

            coEvery { ressursService.hent(uuid) } returns null

            client
                .testGet(
                    "/dagpenger/datadeling/v1/periode/$uuid",
                    token = issueMaskinportenToken(),
                ).status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `returnerer ressurs om den finnes`() =
        testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
            val uuid = UUID.randomUUID()
            val ressurs =
                enRessurs(
                    uuid = uuid,
                    status = RessursStatus.FERDIG,
                    data =
                        DatadelingResponseAfpDTO(
                            personIdent = FNR,
                            listOf(
                                PeriodeAfpDTO(
                                    fraOgMedDato = 1.januar(2023),
                                    ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                                ),
                            ),
                        ),
                )

            coEvery { ressursService.hent(uuid) } returns ressurs

            client.testGet("/dagpenger/datadeling/v1/periode/$uuid", token = issueMaskinportenToken()).let { response ->
                response.status shouldBe HttpStatusCode.OK
                //language=JSON
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "uuid": "$uuid",
                      "status": "FERDIG",
                      "response": {
                        "personIdent": "01020312342",
                        "perioder": [
                          {
                            "fraOgMedDato": "2023-01-01",
                            "ytelseType": "DAGPENGER_ARBEIDSSOKER_ORDINAER"
                          }
                        ]
                      }
                    }
                                                                                                                                      
                    """.trimIndent()
            }
        }
}
