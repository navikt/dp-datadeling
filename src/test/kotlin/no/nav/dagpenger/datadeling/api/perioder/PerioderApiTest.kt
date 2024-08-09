package no.nav.dagpenger.datadeling.api.perioder

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.TestApplication.issueMaskinportenToken
import no.nav.dagpenger.datadeling.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.sporing.AuditHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPeriodeHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPeriodeSpørringHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.datadeling.sporing.NoopLogger
import no.nav.dagpenger.datadeling.testGet
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.FNR
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
    fun `returnerer 401 uten token`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/periode", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 500 hvis oppretting av ressurs feiler`() =
        testPerioderEndpoint {
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
        testPerioderEndpoint {
            val ressurs = enRessurs()
            coEvery { ressursService.opprett(any()) } returns ressurs
            coEvery { perioderService.hentDagpengeperioder(any()) } returns enDatadelingResponse()

            client.testPost("/dagpenger/datadeling/v1/periode", enDatadelingRequest(), issueMaskinportenToken())
                .apply { assertEquals(HttpStatusCode.Created, this.status) }
                .bodyAsText()
                .apply { assertEquals("http://localhost:8080/dagpenger/datadeling/v1/periode/${ressurs.uuid}", this) }
        }

    @Test
    fun `returnerer 404 om en ressus ikke finnes`() {
        testPerioderEndpoint {
            val uuid = UUID.randomUUID()

            coEvery { ressursService.hent(uuid) } returns null

            client.testGet(
                "/dagpenger/datadeling/v1/periode/$uuid",
                token = issueMaskinportenToken(),
            ).status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `returnerer ressurs om den finnes`() =
        testPerioderEndpoint {
            val uuid = UUID.randomUUID()
            val ressurs =
                enRessurs(
                    uuid = uuid,
                    status = RessursStatus.FERDIG,
                    data = enDatadelingResponse(enPeriode(fraOgMed = 1.januar(), tilOgMed = null)),
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

    @Test
    fun `Audit og Sporing logger ved opprettelse av ressurs`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }
        testPerioderEndpoint(logger) {
            val ressurs = enRessurs()
            coEvery { ressursService.opprett(any()) } returns ressurs

            client.testPost(
                "/dagpenger/datadeling/v1/periode",
                enDatadelingRequest(),
                issueMaskinportenToken(orgNummer = "0192:889640782"),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerPeriodeSpørringHendelse>()
                it.ident() shouldBe FNR
                // todo test orgnummer
            }
        }
    }

    @Test
    fun `Audit og Sporing logging ved henting av ressurs`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }
        val uuid = UUID.randomUUID()
        testPerioderEndpoint(logger) {
            coEvery { ressursService.hent(uuid) } returns
                enRessurs(
                    uuid = uuid,
                    status = RessursStatus.FERDIG,
                    request = enDatadelingRequest(fnr = FNR),
                    data = enDatadelingResponse(),
                )

            client.testGet(
                "/dagpenger/datadeling/v1/periode/$uuid",
                issueMaskinportenToken(orgNummer = "0192:889640782"),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerPeriodeHentetHendelse>()
                it.ident() shouldBe FNR
                // todo test ressurs
            }
        }
    }

    private fun testPerioderEndpoint(
        auditLogger: Log = NoopLogger,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        withMockAuthServerAndTestApplication(moduleFunction = {
            konfigurerApi(appConfig = Config.appConfig)
        }) {
            routing { perioderRoutes(ressursService, perioderService, auditLogger) }
            block()
        }
    }
}
