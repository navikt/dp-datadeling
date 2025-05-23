package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.testutil.emptyResponse
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.datadeling.testutil.enPeriode
import no.nav.dagpenger.datadeling.testutil.januar
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_PERMITTERING_ORDINAER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PerioderServiceTest {
    private val proxyClient = mockk<ProxyClient>()
    private val perioderService = PerioderService(proxyClient)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen perioder`() =
        runTest {
            coEvery { proxyClient.hentDagpengeperioder(any()) } returns emptyResponse()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = perioderService.hentDagpengeperioder(request)

            assertEquals(emptyList<Periode>(), response.perioder)
        }

    @Test
    fun `én periode`() =
        runTest {
            val request = enDatadelingRequest(1.januar()..6.januar())
            val response = enDatadelingResponse(enPeriode(1.januar()..6.januar()))

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            assertEquals(response.perioder, perioderService.hentDagpengeperioder(request).perioder)
        }

    @Test
    fun `slår sammen perioder uten gap`() =
        runTest {
            val request = enDatadelingRequest(1.januar()..11.januar())

            val response =
                enDatadelingResponse(
                    enPeriode(1.januar()..6.januar()),
                    enPeriode(7.januar()..11.januar()),
                )

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            perioderService.hentDagpengeperioder(request).let {
                assertEquals(1, it.perioder.size)
                assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
                assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
            }
        }

    @Test
    fun `slår ikke sammen perioder med gap`() =
        runTest {
            val request = enDatadelingRequest(1.januar()..11.januar())

            val response =
                enDatadelingResponse(
                    enPeriode(1.januar()..5.januar()),
                    enPeriode(7.januar()..11.januar()),
                )

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            perioderService.hentDagpengeperioder(request).let {
                assertEquals(2, it.perioder.size)
                assertEquals(response.perioder, it.perioder)
            }
        }

    @Test
    fun `slår ikke sammen perioder med forskjellige ytelsestyper`() =
        runTest {
            val request = enDatadelingRequest(1.januar()..11.januar())

            val response =
                enDatadelingResponse(
                    enPeriode(1.januar()..6.januar(), DAGPENGER_PERMITTERING_ORDINAER),
                    enPeriode(7.januar()..11.januar(), DAGPENGER_ARBEIDSSOKER_ORDINAER),
                )

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            perioderService.hentDagpengeperioder(request).let {
                assertEquals(2, it.perioder.size)
                assertEquals(response.perioder, it.perioder)
            }
        }

    @Test
    fun `avkorter perioden mot forespørsel`() =
        runTest {
            val request = enDatadelingRequest(3.januar()..8.januar())
            val response = enDatadelingResponse(enPeriode(1.januar()..11.januar()))

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            perioderService.hentDagpengeperioder(request).let {
                assertEquals(1, it.perioder.size)
                assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
                assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
            }
        }

    @Test
    fun `avkorter perioder uten avsluttet ytelse`() =
        runTest {
            val request = enDatadelingRequest(3.januar()..8.januar())
            val response = enDatadelingResponse(enPeriode(fraOgMed = 1.januar(), tilOgMed = null))

            coEvery { proxyClient.hentDagpengeperioder(request) } returns response

            perioderService.hentDagpengeperioder(request).let {
                assertEquals(1, it.perioder.size)
                assertEquals(request.fraOgMedDato, it.perioder.first().fraOgMedDato)
                assertEquals(request.tilOgMedDato, it.perioder.first().tilOgMedDato)
            }
        }
}
