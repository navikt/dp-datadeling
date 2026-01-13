package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.dato.februar
import no.nav.dagpenger.dato.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PerioderServiceTest {
    private val ident = "01020312345"
    private val proxyClient = mockk<ProxyClientArena>()
    private val behandlingResultatRepositoryPostgresql = mockk<BehandlingResultatRepositoryMedTolker>()
    private val perioderService = PerioderService(proxyClient, behandlingResultatRepositoryPostgresql)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen perioder`() {
        coEvery { proxyClient.hentDagpengeperioder(any()) } returns emptyList()
        coEvery { behandlingResultatRepositoryPostgresql.hentDagpengeperioder(any()) } returns emptyList()

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = perioderService.hentDagpengeperioder(request)

        assertEquals(emptyList<PeriodeDTO>(), response.perioder)
    }

    @Test
    fun `én periode`() {
        val request = enDatadelingRequest(1.januar(2025)..6.januar(2025))
        val response =
            listOf(
                Periode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = 6.januar(2025),
                    ytelseType = YtelseType.Permittering,
                    kilde = Fagsystem.ARENA,
                ),
            )

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hentDagpengeperioder(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).perioder shouldHaveSize 1
    }

    @Test
    fun `inkluderer kun perioder som ligger innenfor ønsket vindu`() {
        val response =
            listOf(
                Periode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = 5.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 6.januar(2025),
                    tilOgMed = 10.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 15.januar(2025),
                    tilOgMed = 20.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 25.januar(2025),
                    tilOgMed = 30.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 5.februar(2025),
                    tilOgMed = null,
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
            )
        val request = enDatadelingRequest(8.januar(2025)..26.januar(2025))

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hentDagpengeperioder(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            it.perioder shouldHaveSize 3

            it.perioder[0].fraOgMedDato shouldBe 6.januar(2025)
            it.perioder[1].fraOgMedDato shouldBe 15.januar(2025)
            it.perioder[2].fraOgMedDato shouldBe 25.januar(2025)
        }
    }

    @Test
    fun `kan avgrense perioder`() {
        val response =
            listOf(
                Periode(
                    fraOgMed = 1.januar(2025),
                    tilOgMed = 5.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 6.januar(2025),
                    tilOgMed = 10.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 15.januar(2025),
                    tilOgMed = 20.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 25.januar(2025),
                    tilOgMed = 30.januar(2025),
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
                Periode(
                    fraOgMed = 5.februar(2025),
                    tilOgMed = null,
                    ytelseType = YtelseType.Ordinær,
                    kilde = Fagsystem.ARENA,
                ),
            )
        val request = enDatadelingRequest(8.januar(2025)..26.januar(2025))

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hentDagpengeperioder(any()) } returns emptyList()

        perioderService.hentDagpengeperioderAvgrenset(request).let {
            it.perioder shouldHaveSize 3

            it.perioder[0].fraOgMedDato shouldBe 8.januar(2025)
            it.perioder[0].tilOgMedDato shouldBe 10.januar(2025)
            it.perioder[1].fraOgMedDato shouldBe 15.januar(2025)
            it.perioder[1].tilOgMedDato shouldBe 20.januar(2025)
            it.perioder[2].fraOgMedDato shouldBe 25.januar(2025)
            it.perioder[2].tilOgMedDato shouldBe 26.januar(2025)
        }
    }

    private fun enDatadelingRequest(
        periode: ClosedRange<LocalDate>,
        fnr: String = ident,
    ) = DatadelingRequestDTO(
        fraOgMedDato = periode.start,
        tilOgMedDato = periode.endInclusive,
        personIdent = fnr,
    )
}
