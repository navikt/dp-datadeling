package no.nav.dagpenger.behandling

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.behandling.arena.Vedtak
import no.nav.dagpenger.behandling.arena.VedtakService
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.StonadTypeDTO
import no.nav.dagpenger.dato.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceTest {
    private val ident = "01020312345"
    private val proxyClient = mockk<ProxyClientArena>()
    private val vedtakService = VedtakService(proxyClient)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen vedtak`() {
        coEvery { proxyClient.hentVedtak(any()) } returns emptyList()

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = vedtakService.hentVedtak(request)

        assertEquals(emptyList<Vedtak>(), response)
    }

    @Test
    fun `ett vedtak`() {
        val vedtak =
            listOf(
                Vedtak(
                    "VedtakId",
                    "FagsakId",
                    Vedtak.Utfall.INNVILGET,
                    StonadTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    LocalDate.now(),
                    LocalDate.now(),
                ),
            )
        coEvery { proxyClient.hentVedtak(any()) } returns vedtak

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = vedtakService.hentVedtak(request)

        assertEquals(vedtak, response)
    }

    @Test
    fun `flere vedtak`() {
        val vedtak =
            listOf(
                Vedtak(
                    "VedtakId1",
                    "FagsakId1",
                    Vedtak.Utfall.INNVILGET,
                    StonadTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    LocalDate.now(),
                    LocalDate.now(),
                    1111,
                ),
                Vedtak(
                    "VedtakId2",
                    "FagsakId2",
                    Vedtak.Utfall.AVSLÅTT,
                    StonadTypeDTO.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
                    LocalDate.now(),
                    LocalDate.now(),
                    2222,
                    333,
                ),
            )
        coEvery { proxyClient.hentVedtak(any()) } returns vedtak

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = vedtakService.hentVedtak(request)

        assertEquals(vedtak, response)
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
