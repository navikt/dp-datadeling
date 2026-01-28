package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.ktor.client.statement.bodyAsText
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.arena.ArenaBeregning
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.dato.januar
import kotlin.test.Test

class ArenaRoutesTest {
    private val arenaClient: ProxyClientArena = mockk(relaxed = true)

    @Test
    fun `returnerer 200 og DatadelingResponse`() =
        testEndepunkter(arenaClient = arenaClient) {
            val response =
                listOf(
                    ArenaBeregning(
                        meldekortFraDato = 1.januar(2018),
                        meldekortTilDato = 14.januar(2019),
                        innvilgetSats = 800.toBigDecimal(),
                        posteringSats = 500.toBigDecimal(),
                        utbetalingsgrad = 20.toBigDecimal(),
                        belop = 5000.toBigDecimal(),
                    ),
                )
            coEvery { arenaClient.hentBeregninger(any()) } returns response

            client
                .testPost(
                    "/arena/datadeling/v1/beregninger",
                    enDatadelingRequest(),
                    issueAzureToken(
                        azpRoles = listOf(Tilgangsrolle.beregninger.name),
                    ),
                ).bodyAsText()
                .apply {
                    // language=JSON
                    this.shouldEqualJson(
                        """
                        [
                          {
                            "fraOgMed": "2018-01-01",
                            "tilOgMed": "2019-01-14",
                            "innvilgetSats": 800.0,
                            "beregnetSats": 500.0,
                            "utbetaltBeløp": 5000.0,
                            "gjenståendeDager": 260
                          }
                        ]
                        """.trimIndent(),
                    )
                }
        }
}
