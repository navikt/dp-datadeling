package no.nav.dagpenger.datadeling.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.service.InnsynService
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.sporing.AuditHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSøknaderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerVedtakHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.datadeling.sporing.NoopLogger
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_PERMITTERING_ORDINAER
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DagpengerRoutesTest {
    private val perioderService: PerioderService = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk(relaxed = true)

    @Test
    fun `returnerer 401 uten token for perioder`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/perioder", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for soknader`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/soknader", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for vedtak`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/vedtak", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for perioder`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/perioder", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for soknader`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/soknader", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for vedtak`() =
        testPerioderEndpoint {
            client.testPost("/dagpenger/datadeling/v1/vedtak", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 200 og DatadelingResponse`() =
        testPerioderEndpoint {
            val response =
                enDatadelingResponse(
                    Periode(fraOgMedDato = LocalDate.now(), ytelseType = DAGPENGER_ARBEIDSSOKER_ORDINAER),
                    Periode(
                        fraOgMedDato = LocalDate.now().minusDays(100),
                        tilOgMedDato = LocalDate.now().minusDays(1),
                        ytelseType = DAGPENGER_PERMITTERING_ORDINAER,
                    ),
                )
            coEvery { perioderService.hentDagpengeperioder(any()) } returns response

            client
                .testPost("/dagpenger/datadeling/v1/perioder", enDatadelingRequest(), issueAzureToken())
                .bodyAsText()
                .apply { assertEquals(objectMapper.writeValueAsString(response), this) }
        }

    @Test
    fun `Audit og Sporing logger ved henting av perioder`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }

        val fraOgMedDato = LocalDate.now().minusDays(100)
        val tilOgMedDato = LocalDate.now().minusDays(1)
        val request = enDatadelingRequest(fraOgMed = fraOgMedDato, tilOgMed = tilOgMedDato)

        val response =
            enDatadelingResponse(
                Periode(fraOgMedDato = LocalDate.now(), ytelseType = DAGPENGER_ARBEIDSSOKER_ORDINAER),
                Periode(
                    fraOgMedDato = LocalDate.now().minusDays(100),
                    tilOgMedDato = LocalDate.now().minusDays(1),
                    ytelseType = DAGPENGER_PERMITTERING_ORDINAER,
                ),
            )
        coEvery { perioderService.hentDagpengeperioder(any()) } returns response

        testPerioderEndpoint(logger) {
            client.testPost(
                "/dagpenger/datadeling/v1/perioder",
                request,
                issueAzureToken(),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerPerioderHentetHendelse>()
                it.ident() shouldBe FNR
                it.request shouldBe request
                it.response shouldBe response
            }
        }
    }

    @Test
    fun `Audit og Sporing logger ved henting av soknader`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }

        val fraOgMedDato = LocalDate.now().minusDays(100)
        val tilOgMedDato = LocalDate.now().minusDays(1)
        val request = enDatadelingRequest(fraOgMed = fraOgMedDato, tilOgMed = tilOgMedDato)

        val response =
            listOf(
                Søknad(
                    UUID.randomUUID().toString(),
                    "1",
                    "2",
                    Søknad.SøknadsType.NySøknad,
                    Søknad.Kanal.Digital,
                    LocalDateTime.now(),
                ),
                Søknad(
                    UUID.randomUUID().toString(),
                    "2",
                    "3",
                    Søknad.SøknadsType.Gjenopptak,
                    Søknad.Kanal.Digital,
                    LocalDateTime.now(),
                ),
            )
        coEvery { innsynService.hentSoknader(any()) } returns response

        testPerioderEndpoint(logger) {
            client.testPost(
                "/dagpenger/datadeling/v1/soknader",
                request,
                issueAzureToken(),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerSøknaderHentetHendelse>()
                it.ident() shouldBe FNR
                it.request shouldBe request
                it.response shouldBe response
            }
        }
    }

    @Test
    fun `Audit og Sporing logger ved henting av vedtak`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }

        val fraOgMedDato = LocalDate.now().minusDays(100)
        val tilOgMedDato = LocalDate.now().minusDays(1)
        val request = enDatadelingRequest(fraOgMed = fraOgMedDato, tilOgMed = tilOgMedDato)

        val response =
            listOf(
                Vedtak(
                    "1",
                    "2",
                    Vedtak.Status.AVSLÅTT,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                ),
                Vedtak(
                    "2",
                    "3",
                    Vedtak.Status.INNVILGET,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                ),
            )
        coEvery { innsynService.hentVedtak(any()) } returns response

        testPerioderEndpoint(logger) {
            client.testPost(
                "/dagpenger/datadeling/v1/vedtak",
                request,
                issueAzureToken(),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerVedtakHentetHendelse>()
                it.ident() shouldBe FNR
                it.request shouldBe request
                it.response shouldBe response
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
            routing { dagpengerRoutes(perioderService, innsynService, auditLogger) }
            block()
        }
    }
}
