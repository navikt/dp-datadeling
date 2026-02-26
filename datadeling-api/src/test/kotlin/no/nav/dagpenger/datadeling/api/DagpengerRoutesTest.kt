package no.nav.dagpenger.datadeling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
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
}
