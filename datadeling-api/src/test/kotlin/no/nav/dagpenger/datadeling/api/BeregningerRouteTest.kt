package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.BehandlingResultat
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.BeregnetDag
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import kotlin.test.Test

class BeregningerRouteTest {
    @Test
    fun `returnerer 200 og beregninger`() {
        val behandlinger = mockk<BehandlingResultatRepositoryMedTolker>()
        val behandling =
            mockk<BehandlingResultat>().apply {
                every { beregninger } returns
                    listOf(
                        TestBeregnetDag(
                            dato = LocalDate.of(2024, 1, 1),
                            sats = 500,
                            utbetaling = 15000,
                            gjenståendeDager = 10,
                        ),
                        TestBeregnetDag(
                            dato = LocalDate.of(2024, 2, 1),
                            sats = 600,
                            utbetaling = 18000,
                            gjenståendeDager = 9,
                        ),
                    )
            }
        every { behandlinger.hent(any()) } returns listOf(behandling)

        testEndepunkter(behandlingRepository = behandlinger) {
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

    private data class TestBeregnetDag(
        override val dato: LocalDate,
        override val sats: Int,
        override val utbetaling: Int,
        override val gjenståendeDager: Int = 0,
    ) : BeregnetDag
}
