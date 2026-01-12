package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.BehandlingResultat
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.Utbetaling
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import kotlin.test.Test

class UtbetalingRouteTest {
    @Test
    fun `returnerer 200 og utbetalinger`() {
        val behandlinger = mockk<BehandlingResultatRepositoryMedTolker>()
        val behandling =
            mockk<BehandlingResultat>().apply {
                every { utbetalinger } returns
                    listOf(
                        TestUtbetaling(
                            dato = LocalDate.of(2024, 1, 1),
                            sats = 500,
                            utbetaling = 15000,
                        ),
                        TestUtbetaling(
                            dato = LocalDate.of(2024, 2, 1),
                            sats = 600,
                            utbetaling = 18000,
                        ),
                    )
            }
        every { behandlinger.hent(any()) } returns listOf(behandling)

        testEndepunkter(behandlingRepository = behandlinger) {
            val response =
                client.testPost(
                    "/dagpenger/datadeling/v1/utbetaling",
                    enDatadelingRequest(),
                    issueAzureToken(azpRoles = listOf(Tilgangsrolle.utbetaling.name)),
                )

            response.status shouldBe HttpStatusCode.OK
            @Language("JSON")
            response.bodyAsText() shouldEqualJson
                """
                [
                  {
                    "dato": "2024-01-01",
                    "sats": 500,
                    "utbetaltBeløp": 15000
                  },
                  {
                    "dato": "2024-02-01",
                    "utbetaltBeløp": 18000,
                    "sats": 600
                  }
                ] 
                """.trimIndent()
        }
    }

    private data class TestUtbetaling(
        override val dato: LocalDate,
        override val sats: Int,
        override val utbetaling: Int,
    ) : Utbetaling
}
