package no.nav.dagpenger.behandling

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.behandling.arena.ArenaBeregning
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.dato.mars
import org.junit.jupiter.api.Disabled
import java.time.LocalDate
import kotlin.test.Test

class BeregningerServiceTest {
    @Disabled("Arena har ikke tjenesten i prod")
    @Test
    fun `returnerer beregninger fra både Arena og dp-sak med filtrering på periode`() =
        runBlocking {
            val arenaClient = mockk<ProxyClientArena>()
            val dpSakRepository = mockk<BehandlingResultatRepositoryMedTolker>()
            val beregningerService = BeregningerService(arenaClient, dpSakRepository)
            coEvery { arenaClient.hentBeregninger(any()) } returns
                listOf(
                    ArenaBeregning(
                        meldekortFraDato = 1.mars(2023),
                        meldekortTilDato = 14.mars(2023),
                        innvilgetSats = 800.toBigDecimal(),
                        belop = 1000.toBigDecimal(),
                        antallDagerGjenstående = 260.toBigDecimal(),
                    ),
                    ArenaBeregning(
                        meldekortFraDato = 1.januar(2024),
                        meldekortTilDato = 14.januar(2024),
                        innvilgetSats = 800.toBigDecimal(),
                        belop = 1000.toBigDecimal(),
                        antallDagerGjenstående = 250.toBigDecimal(),
                    ),
                    ArenaBeregning(
                        meldekortFraDato = 15.januar(2024),
                        meldekortTilDato = 29.januar(2024),
                        innvilgetSats = 800.toBigDecimal(),
                        belop = 1000.toBigDecimal(),
                        antallDagerGjenstående = 240.toBigDecimal(),
                    ),
                )
            coEvery { dpSakRepository.hent(any()) } returns
                listOf(
                    mockk<BehandlingResultat>().apply {
                        every { beregninger } returns
                            listOf(
                                TestBeregnetDag(
                                    dato = 30.januar(2024),
                                    sats = 600,
                                    utbetaling = 1000,
                                    gjenståendeDager = 10,
                                ),
                                TestBeregnetDag(
                                    dato = 31.januar(2024),
                                    sats = 600,
                                    utbetaling = 1000,
                                    gjenståendeDager = 9,
                                ),
                            )
                    },
                )

            val scenarier =
                listOf(
                    10.januar(2023)..30.januar(2025) to 5000,
                    10.januar(2023)..LocalDate.MAX to 5000,
                    10.januar(2024)..30.januar(2024) to 3000,
                    10.januar(2024)..31.januar(2024) to 4000,
                    2.januar(2024)..5.januar(2024) to 1000,
                    2.januar(2024)..16.januar(2024) to 2000,
                )

            scenarier.forEach { (periode, sum) ->
                withClue("Periode $periode skal gi sum $sum") {
                    val request = DatadelingRequestDTO("123", periode.start, periode.endInclusive)
                    val response = beregningerService.hentBeregninger(request)

                    response.sumOf { it.utbetaltBeløp } shouldBe sum
                }
            }
        }

    private data class TestScenario(
        val navn: String,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate? = LocalDate.MAX,
        val forventetSum: Int,
    )
}

private data class TestBeregnetDag(
    override val dato: LocalDate,
    override val sats: Int,
    override val utbetaling: Int,
    override val gjenståendeDager: Int,
) : BeregnetDag
