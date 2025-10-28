package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.models.StonadTypeDTO
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceTest {
    private val proxyClient = mockk<ProxyClient>()
    private val vedtakService = VedtakService(proxyClient)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen vedtak`() =
        runTest {
            coEvery { proxyClient.hentVedtak(any()) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(emptyList<Vedtak>(), response)
        }

    @Test
    fun `ett vedtak`() =
        runTest {
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

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }

    @Test
    fun `flere vedtak`() =
        runTest {
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

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = vedtakService.hentVedtak(request)

            assertEquals(vedtak, response)
        }
}
