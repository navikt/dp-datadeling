package no.nav.dagpenger.datadeling.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.behandling.arena.Vedtak
import no.nav.dagpenger.behandling.arena.VedtakService
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTOKildeDTO
import no.nav.dagpenger.datadeling.models.StonadTypeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.sporing.AuditHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerMeldekortHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSisteSøknadHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSøknaderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerVedtakHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testPostText
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.søknad.SøknadService
import no.nav.dagpenger.søknad.modell.Søknad
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DagpengerRoutesTest {
    private val perioderService: PerioderService = mockk(relaxed = true)
    private val meldekortservice: MeldekortService = mockk(relaxed = true)
    private val søknaderService: SøknadService = mockk(relaxed = true)
    private val vedtakService: VedtakService = mockk(relaxed = true)

    @Test
    fun `returnerer 401 uten token for perioder`() =
        testEndepunkter {
            client.testPost("/dagpenger/datadeling/v1/perioder", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for meldekort`() =
        testEndepunkter {
            client.testPost("/dagpenger/datadeling/v1/meldekort", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 når Meldekort-liste hentes uten riktig rolle`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/meldekort",
                    enDatadelingRequest(),
                    issueAzureToken(azpRoles = listOf("ROLE")),
                ).status shouldBe HttpStatusCode.Forbidden
        }

    @Test
    fun `returnerer 401 uten token for soknader`() =
        testEndepunkter {
            client.testPost("/dagpenger/datadeling/v1/soknader", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for siste soknad`() =
        testEndepunkter {
            client.testPost("/dagpenger/datadeling/v1/siste_soknad", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 401 uten token for vedtak`() =
        testEndepunkter {
            client.testPost("/dagpenger/datadeling/v1/vedtak", enDatadelingRequest(), token = null).apply {
                assertEquals(HttpStatusCode.Unauthorized, this.status)
            }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for perioder`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/perioder",
                    "",
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.rettighetsperioder.name),
                    ),
                ).apply {
                    assertEquals(HttpStatusCode.BadRequest, this.status)
                }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for meldekort`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/meldekort",
                    "",
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.meldekort.name)),
                ).apply {
                    assertEquals(HttpStatusCode.BadRequest, this.status)
                }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for soknader`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/soknader",
                    "",
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.soknad.name),
                    ),
                ).apply {
                    assertEquals(HttpStatusCode.BadRequest, this.status)
                }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for siste søknad`() =
        testEndepunkter {
            client
                .testPostText(
                    "/dagpenger/datadeling/v1/siste_soknad",
                    "",
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.soknad.name),
                    ),
                ).apply {
                    assertEquals(HttpStatusCode.BadRequest, this.status)
                }
        }

    @Test
    fun `returnerer 400 hvis ikke kan prosessere request for vedtak`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/vedtak",
                    "",
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.vedtak.name),
                    ),
                ).apply {
                    assertEquals(HttpStatusCode.BadRequest, this.status)
                }
        }

    @Test
    fun `returnerer 200 og DatadelingResponse`() =
        testEndepunkter(perioderService = perioderService) {
            val response =
                enDatadelingResponse(
                    PeriodeDTO(
                        fraOgMedDato = LocalDate.now(),
                        ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        kilde = PeriodeDTOKildeDTO.ARENA,
                    ),
                    PeriodeDTO(
                        fraOgMedDato = LocalDate.now().minusDays(100),
                        tilOgMedDato = LocalDate.now().minusDays(1),
                        ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        kilde = PeriodeDTOKildeDTO.ARENA,
                    ),
                )
            coEvery { perioderService.hentDagpengeperioder(any()) } returns response

            client
                .testPost(
                    "/dagpenger/datadeling/v1/perioder",
                    enDatadelingRequest(),
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.rettighetsperioder.name),
                    ),
                ).bodyAsText()
                .apply { assertEquals(objectMapper.writeValueAsString(response), this) }
        }

    @Test
    fun `returnerer 200 og Meldekort-liste`() =
        testEndepunkter {
            val response = listOf<MeldekortDTO>()
            coEvery { meldekortservice.hentMeldekort(any()) } returns response

            client
                .testPost(
                    "/dagpenger/datadeling/v1/meldekort",
                    enDatadelingRequest(),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.meldekort.name)),
                ).bodyAsText()
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
                PeriodeDTO(
                    fraOgMedDato = LocalDate.now(),
                    ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    kilde = PeriodeDTOKildeDTO.ARENA,
                ),
                PeriodeDTO(
                    fraOgMedDato = LocalDate.now().minusDays(100),
                    tilOgMedDato = LocalDate.now().minusDays(1),
                    ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                    kilde = PeriodeDTOKildeDTO.ARENA,
                ),
            )
        coEvery { perioderService.hentDagpengeperioder(any()) } returns response

        testEndepunkter(auditLogger = logger, perioderService = perioderService) {
            client.testPost(
                "/dagpenger/datadeling/v1/perioder",
                request,
                issueAzureToken(
                    azpRoles = listOf(Tilgangsrolle.rettighetsperioder.name),
                ),
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

        testEndepunkter(auditLogger = logger) {
            client.testPost(
                "/dagpenger/datadeling/v1/meldekort",
                request,
                issueAzureToken(azpRoles = listOf(Tilgangsrolle.meldekort.name)),
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

        testEndepunkter(auditLogger = logger, søknadService = søknaderService) {
            client.testPost(
                "/dagpenger/datadeling/v1/soknader",
                request,
                issueAzureToken(
                    azpRoles = listOf(Tilgangsrolle.soknad.name),
                ),
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

        testEndepunkter(auditLogger = logger, søknadService = søknaderService) {
            client.testPostText(
                "/dagpenger/datadeling/v1/siste_soknad",
                FNR,
                issueAzureToken(
                    azpRoles = listOf(Tilgangsrolle.soknad.name),
                ),
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

        testEndepunkter(auditLogger = logger, vedtakService = vedtakService) {
            client.testPost(
                "/dagpenger/datadeling/v1/vedtak",
                request,
                issueAzureToken(
                    azpRoles = listOf(Tilgangsrolle.vedtak.name),
                ),
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
}
