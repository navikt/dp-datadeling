package no.nav.dagpenger.datadeling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.DagpengestatusService
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.models.DagpengestatusResponseDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.meldekort.MeldekortService
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DagpengerRoutesTest {
    private val perioderService: PerioderService = mockk(relaxed = true)
    private val meldekortservice: MeldekortService = mockk(relaxed = true)

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
    fun `returnerer 200 og DatadelingResponse`() =
        testEndepunkter(perioderService = perioderService) {
            val response =
                enDatadelingResponse(
                    PeriodeDTO(
                        fraOgMedDato = LocalDate.now(),
                        ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        kilde = FagsystemDTO.ARENA,
                    ),
                    PeriodeDTO(
                        fraOgMedDato = LocalDate.now().minusDays(100),
                        tilOgMedDato = LocalDate.now().minusDays(1),
                        ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        kilde = FagsystemDTO.ARENA,
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
    fun `dagpengestatus returnerer 200 med dato ved innvilgelse`() {
        val dagpengestatusService: DagpengestatusService = mockk()
        every { dagpengestatusService.hentDagpengestatus(any()) } returns
            DagpengestatusResponseDTO(
                personIdent = "12345678901",
                forsteDagpengevedtakDato = LocalDate.of(2026, 3, 15),
            )

        testEndepunkter(dagpengestatusService = dagpengestatusService) {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/dagpengestatus",
                    mapOf("personIdent" to "12345678901"),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.dagpengestatus.name)),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val body = objectMapper.readTree(bodyAsText())
                    body["personIdent"].asText() shouldBe "12345678901"
                    body["forsteDagpengevedtakDato"].asText() shouldBe "2026-03-15"
                }
        }
    }

    @Test
    fun `dagpengestatus returnerer 200 med null dato når person ikke finnes i ny løsning`() {
        val dagpengestatusService: DagpengestatusService = mockk()
        every { dagpengestatusService.hentDagpengestatus(any()) } returns
            DagpengestatusResponseDTO(personIdent = "12345678901")

        testEndepunkter(dagpengestatusService = dagpengestatusService) {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/dagpengestatus",
                    mapOf("personIdent" to "12345678901"),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.dagpengestatus.name)),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val body = objectMapper.readTree(bodyAsText())
                    body["personIdent"].asText() shouldBe "12345678901"
                    body.has("forsteDagpengevedtakDato") shouldBe true
                    body["forsteDagpengevedtakDato"].isNull shouldBe true
                }
        }
    }

    @Test
    fun `dagpengestatus returnerer 401 uten token`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/dagpengestatus",
                    mapOf("personIdent" to "12345678901"),
                    token = null,
                ).status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `dagpengestatus returnerer 403 uten riktig rolle`() =
        testEndepunkter {
            client
                .testPost(
                    "/dagpenger/datadeling/v1/dagpengestatus",
                    mapOf("personIdent" to "12345678901"),
                    issueAzureToken(azpRoles = listOf("FEIL_ROLLE")),
                ).status shouldBe HttpStatusCode.Forbidden
        }
}
