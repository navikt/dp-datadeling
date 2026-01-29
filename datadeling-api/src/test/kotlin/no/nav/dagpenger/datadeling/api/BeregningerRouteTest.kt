package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.BeregningerService
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.models.BeregnetDagDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import kotlin.test.Test

class BeregningerRouteTest {
    @Test
    fun `returnerer 200 og beregninger fra dp-sak`() {
        val beregningerService = mockk<BeregningerService>()
        coEvery { beregningerService.hentBeregninger(any()) } returns
            listOf(
                BeregnetDagDTO(
                    fraOgMed = LocalDate.of(2024, 1, 1),
                    tilOgMed = LocalDate.of(2024, 1, 1),
                    sats = 500,
                    utbetaltBeløp = 15000,
                    gjenståendeDager = 10,
                    kilde = FagsystemDTO.DP_SAK,
                ),
                BeregnetDagDTO(
                    fraOgMed = LocalDate.of(2024, 2, 1),
                    tilOgMed = LocalDate.of(2024, 2, 1),
                    sats = 600,
                    utbetaltBeløp = 18000,
                    gjenståendeDager = 9,
                    kilde = FagsystemDTO.DP_SAK,
                ),
            )

        testEndepunkter(beregningerService = beregningerService) {
            val response =
                client.testPost(
                    "/dagpenger/datadeling/v1/beregninger",
                    enDatadelingRequest(),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.beregninger.name)),
                )

            response.status shouldBe HttpStatusCode.OK
            @Language("JSON")
            response.bodyAsText() shouldEqualJson
                """
                [
                  {
                    "fraOgMed": "2024-01-01",
                    "tilOgMed": "2024-01-01",
                    "sats": 500,
                    "utbetaltBeløp": 15000,
                    "gjenståendeDager": 10,
                    "kilde": "DP_SAK"
                  },
                  {
                    "fraOgMed": "2024-02-01",
                    "tilOgMed": "2024-02-01",
                    "utbetaltBeløp": 18000,
                    "sats": 600,
                    "gjenståendeDager": 9,
                    "kilde": "DP_SAK"
                  }
                ] 
                """.trimIndent()
        }
    }

    @Test
    fun `returnerer beregninger fra både Arena og dp-sak`() {
        val beregningerService = mockk<BeregningerService>()
        coEvery { beregningerService.hentBeregninger(any()) } returns
            listOf(
                BeregnetDagDTO(
                    fraOgMed = LocalDate.of(2024, 1, 1),
                    tilOgMed = LocalDate.of(2024, 1, 14),
                    sats = 800,
                    utbetaltBeløp = 5000,
                    gjenståendeDager = 260,
                    kilde = FagsystemDTO.ARENA,
                ),
                BeregnetDagDTO(
                    fraOgMed = LocalDate.of(2024, 2, 1),
                    tilOgMed = LocalDate.of(2024, 2, 1),
                    sats = 600,
                    utbetaltBeløp = 18000,
                    gjenståendeDager = 9,
                    kilde = FagsystemDTO.DP_SAK,
                ),
            )

        testEndepunkter(beregningerService = beregningerService) {
            val response =
                client.testPost(
                    "/dagpenger/datadeling/v1/beregninger",
                    enDatadelingRequest(),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.beregninger.name)),
                )

            response.status shouldBe HttpStatusCode.OK
            @Language("JSON")
            response.bodyAsText() shouldEqualJson
                """
                [
                  {
                    "fraOgMed": "2024-01-01",
                    "tilOgMed": "2024-01-14",
                    "sats": 800,
                    "utbetaltBeløp": 5000,
                    "gjenståendeDager": 260,
                    "kilde": "ARENA"
                  },
                  {
                    "fraOgMed": "2024-02-01",
                    "tilOgMed": "2024-02-01",
                    "utbetaltBeløp": 18000,
                    "sats": 600,
                    "gjenståendeDager": 9,
                    "kilde": "DP_SAK"
                  }
                ] 
                """.trimIndent()
        }
    }
}
