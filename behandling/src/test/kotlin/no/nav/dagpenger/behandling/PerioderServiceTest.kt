package no.nav.dagpenger.behandling

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.arena.ProxyClient
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.dato.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PerioderServiceTest {
    private val ident = "01020312345"
    private val proxyClient = mockk<ProxyClient>()
    private val behandlingResultatRepositoryPostgresql = mockk<BehandlingResultatRepository>()
    private val perioderService = PerioderService(proxyClient, behandlingResultatRepositoryPostgresql)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen perioder`() {
        coEvery { proxyClient.hentDagpengeperioder(any()) } returns
            DatadelingResponseDTO(
                personIdent = ident,
                perioder = emptyList(),
            )
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = perioderService.hentDagpengeperioder(request)

        assertEquals(emptyList<PeriodeDTO>(), response.perioder)
    }

    @Test
    fun `én periode`() {
        val request = enDatadelingRequest(1.januar(2025)..6.januar(2025))
        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                perioder =
                    listOf(
                        PeriodeDTO(
                            fraOgMedDato = 1.januar(2025),
                            tilOgMedDato = 6.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                    ),
            )

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        assertEquals(response.perioder, perioderService.hentDagpengeperioder(request).perioder)
    }

    @Test
    fun `slår sammen perioder uten gap`() {
        val request = enDatadelingRequest(1.januar(2025)..11.januar(2025))
        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                perioder =
                    listOf(
                        PeriodeDTO(
                            fraOgMedDato = 1.januar(2025),
                            tilOgMedDato = 6.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                        PeriodeDTO(
                            fraOgMedDato = 7.januar(2025),
                            tilOgMedDato = 11.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                    ),
            )
        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            assertEquals(1, it.perioder.size)
            assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
            assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
        }
    }

    @Test
    fun `slår ikke sammen perioder med gap`() {
        val request = enDatadelingRequest(1.januar(2025)..11.januar(2025))
        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                perioder =
                    listOf(
                        PeriodeDTO(
                            fraOgMedDato = 1.januar(2025),
                            tilOgMedDato = 5.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                        PeriodeDTO(
                            fraOgMedDato = 7.januar(2025),
                            tilOgMedDato = 11.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                    ),
            )
        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            assertEquals(2, it.perioder.size)
            assertEquals(response.perioder, it.perioder)
        }
    }

    @Test
    fun `slår ikke sammen perioder med forskjellige ytelsestyper`() {
        val request = enDatadelingRequest(1.januar(2025)..11.januar(2025))

        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                perioder =
                    listOf(
                        PeriodeDTO(
                            fraOgMedDato = 1.januar(2025),
                            tilOgMedDato = 6.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                        ),
                        PeriodeDTO(
                            fraOgMedDato = 7.januar(2025),
                            tilOgMedDato = 11.januar(2025),
                            ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        ),
                    ),
            )

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            assertEquals(2, it.perioder.size)
            assertEquals(response.perioder, it.perioder)
        }
    }

    @Test
    fun `avkorter perioden mot forespørsel`() {
        val request = enDatadelingRequest(3.januar(2025)..8.januar(2025))
        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                listOf(
                    PeriodeDTO(
                        fraOgMedDato = 1.januar(2025),
                        tilOgMedDato = 11.januar(2025),
                        ytelseType = YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER,
                    ),
                ),
            )

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            assertEquals(1, it.perioder.size)
            assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
            assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
        }
    }

    @Test
    fun `avkorter perioder uten avsluttet ytelse`() {
        val request = enDatadelingRequest(3.januar(2025)..8.januar(2025))
        val response =
            DatadelingResponseDTO(
                personIdent = ident,
                listOf(
                    PeriodeDTO(
                        fraOgMedDato = 1.januar(2025),
                        tilOgMedDato = null,
                        ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    ),
                ),
            )

        coEvery { proxyClient.hentDagpengeperioder(request) } returns response
        coEvery { behandlingResultatRepositoryPostgresql.hent(any()) } returns emptyList()

        perioderService.hentDagpengeperioder(request).let {
            assertEquals(1, it.perioder.size)
            assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
            assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
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
