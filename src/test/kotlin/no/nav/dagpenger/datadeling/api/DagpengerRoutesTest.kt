package no.nav.dagpenger.datadeling.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.StonadTypeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.service.MeldekortService
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService
import no.nav.dagpenger.datadeling.sporing.AuditHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerMeldekortHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSisteSøknadHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSøknaderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerVedtakHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.datadeling.sporing.NoopLogger
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testPostText
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DagpengerRoutesTest {
    private val perioderService: PerioderService = mockk(relaxed = true)
    private val meldekortservice: MeldekortService = mockk(relaxed = true)
    private val søknaderService: SøknaderService = mockk(relaxed = true)
    private val vedtakService: VedtakService = mockk(relaxed = true)

    @Test
    fun `returnerer 401 uten token for perioder`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/perioder", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for meldekort`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/meldekort", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for soknader`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/soknader", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for siste soknad`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/siste_soknad", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for vedtak`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/vedtak", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for perioder`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/perioder", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for meldekort`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/meldekort", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for soknader`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/soknader", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for siste søknad`() =
        testEndpoint {
            client.testPostText("/dagpenger/datadeling/v1/siste_soknad", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for vedtak`() =
        testEndpoint {
            client.testPost("/dagpenger/datadeling/v1/vedtak", "", issueAzureToken()).apply {
                assertEquals(HttpStatusCode.BadRequest, this.status)
            }
        }

    @Test
    fun `returnerer 200 og DatadelingResponse`() =
        testEndpoint {
            val response =
                enDatadelingResponse(
                    PeriodeDTO(fraOgMedDato = LocalDate.now(), ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER),
                    PeriodeDTO(
                        fraOgMedDato = LocalDate.now().minusDays(100),
                        tilOgMedDato = LocalDate.now().minusDays(1),
                        ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                    ),
                )
            coEvery { perioderService.hentDagpengeperioder(any()) } returns response

            client
                .testPost("/dagpenger/datadeling/v1/perioder", enDatadelingRequest(), issueAzureToken())
                .bodyAsText()
                .apply { assertEquals(objectMapper.writeValueAsString(response), this) }
        }

    @Test
    fun `returnerer 200 og Meldekort-liste`() =
        testEndpoint {
            val response = listOf<MeldekortDTO>()
            coEvery { meldekortservice.hentMeldekort(any()) } returns response

            client
                .testPost("/dagpenger/datadeling/v1/meldekort", enDatadelingRequest(), issueAzureToken())
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
                PeriodeDTO(fraOgMedDato = LocalDate.now(), ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER),
                PeriodeDTO(
                    fraOgMedDato = LocalDate.now().minusDays(100),
                    tilOgMedDato = LocalDate.now().minusDays(1),
                    ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                ),
            )
        coEvery { perioderService.hentDagpengeperioder(any()) } returns response

        testEndpoint(logger) {
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
    fun `Audit og Sporing logger ved henting av meldekort`() {
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

        val response = listOf<MeldekortDTO>()
        coEvery { meldekortservice.hentMeldekort(any()) } returns response

        testEndpoint(logger) {
            client.testPost(
                "/dagpenger/datadeling/v1/meldekort",
                request,
                issueAzureToken(),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerMeldekortHentetHendelse>()
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
        coEvery { søknaderService.hentSøknader(any()) } returns response

        testEndpoint(logger) {
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
    fun `Audit og Sporing logger ved henting av siste søknad`() {
        val logger =
            object : Log {
                val hendelser = mutableListOf<AuditHendelse>()

                override fun log(hendelse: AuditHendelse) {
                    hendelser.add(hendelse)
                }
            }

        val response =
            Søknad(
                UUID.randomUUID().toString(),
                "1",
                "2",
                Søknad.SøknadsType.NySøknad,
                Søknad.Kanal.Digital,
                LocalDateTime.now(),
            )
        coEvery { søknaderService.hentSisteSøknad(any()) } returns response

        testEndpoint(logger) {
            client.testPostText(
                "/dagpenger/datadeling/v1/siste_soknad",
                FNR,
                issueAzureToken(),
            )

            logger.hendelser.size shouldBe 1
            logger.hendelser.first().let {
                it.shouldBeInstanceOf<DagpengerSisteSøknadHentetHendelse>()
                it.ident() shouldBe FNR
                it.request shouldBe FNR
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
                    Vedtak.Utfall.AVSLÅTT,
                    StonadTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                    LocalDate.now(),
                    LocalDate.now(),
                ),
                Vedtak(
                    "2",
                    "3",
                    Vedtak.Utfall.INNVILGET,
                    StonadTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                    LocalDate.now(),
                    LocalDate.now(),
                ),
            )
        coEvery { vedtakService.hentVedtak(any()) } returns response

        testEndpoint(logger) {
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

    private fun testEndpoint(
        auditLogger: Log = NoopLogger,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        withMockAuthServerAndTestApplication(moduleFunction = {
            konfigurerApi(appConfig = Config.appConfig)
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }) {
            routing { dagpengerRoutes(perioderService, meldekortservice, søknaderService, vedtakService, auditLogger) }
            block()
        }
    }
}
