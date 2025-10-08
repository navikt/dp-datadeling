package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.januar
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceTest {
    private val proxyClient = mockk<ProxyClient>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakService = VedtakService(proxyClient, vedtakRepository)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen vedtak`() =
        runTest {
            coEvery { proxyClient.hentVedtak(any()) } returns emptyList()
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(emptyList<Vedtak>(), response)
        }

    @Test
    fun `ett vedtak fra Arena`() =
        runTest {
            val vedtak =
                listOf(
                    Vedtak(
                        "VedtakId",
                        "FagsakId",
                        Vedtak.Utfall.INNVILGET,
                        StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        LocalDate.now(),
                        LocalDate.now(),
                    ),
                )
            coEvery { proxyClient.hentVedtak(any()) } returns vedtak
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }

    @Test
    fun `ett vedtak fra DP`() =
        runTest {
            val vedtak =
                listOf(
                    Vedtak(
                        "VedtakId",
                        "FagsakId",
                        Vedtak.Utfall.INNVILGET,
                        StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        LocalDate.now(),
                        LocalDate.now(),
                    ),
                )
            coEvery { proxyClient.hentVedtak(any()) } returns emptyList()
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns vedtak

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }

    @Test
    fun `flere vedtak fra Arena`() =
        runTest {
            val vedtak =
                listOf(
                    Vedtak(
                        "VedtakId1",
                        "FagsakId1",
                        Vedtak.Utfall.INNVILGET,
                        StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        LocalDate.now(),
                        LocalDate.now(),
                        1111,
                    ),
                    Vedtak(
                        "VedtakId2",
                        "FagsakId2",
                        Vedtak.Utfall.AVSLÅTT,
                        StønadTypeDagpenger.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
                        LocalDate.now(),
                        LocalDate.now(),
                        2222,
                        333,
                    ),
                )
            coEvery { proxyClient.hentVedtak(any()) } returns vedtak
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }

    @Test
    fun `flere vedtak fra DP`() =
        runTest {
            val vedtak =
                listOf(
                    Vedtak(
                        "VedtakId1",
                        "FagsakId1",
                        Vedtak.Utfall.INNVILGET,
                        StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        LocalDate.now(),
                        LocalDate.now(),
                        1111,
                    ),
                    Vedtak(
                        "VedtakId2",
                        "FagsakId2",
                        Vedtak.Utfall.AVSLÅTT,
                        StønadTypeDagpenger.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
                        LocalDate.now(),
                        LocalDate.now(),
                        2222,
                        333,
                    ),
                )
            coEvery { proxyClient.hentVedtak(any()) } returns emptyList()
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns vedtak

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }

    @Test
    fun `vedtak både fra Arena og DP`() =
        runTest {
            val arenaVedtak =
                listOf(
                    Vedtak(
                        "VedtakId1",
                        "FagsakId1",
                        Vedtak.Utfall.INNVILGET,
                        StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                        LocalDate.now(),
                        LocalDate.now(),
                        1111,
                        barnetillegg = 2222,
                    ),
                )
            val dpVedtak =
                listOf(
                    Vedtak(
                        "VedtakId2",
                        "FagsakId2",
                        Vedtak.Utfall.AVSLÅTT,
                        StønadTypeDagpenger.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
                        LocalDate.now(),
                        LocalDate.now(),
                    ),
                )

            coEvery { proxyClient.hentVedtak(any()) } returns arenaVedtak
            coEvery { vedtakRepository.hentVedtakFor(eq(FNR), eq(1.januar()), eq(10.januar())) } returns dpVedtak

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(arenaVedtak + dpVedtak, response)
        }
}
