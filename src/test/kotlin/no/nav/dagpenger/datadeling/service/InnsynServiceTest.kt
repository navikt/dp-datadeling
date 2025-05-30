package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class InnsynServiceTest {
    private val søknadRepository = mockk<SøknadRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val innsynService = InnsynService(søknadRepository, vedtakRepository)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `ingen søknader`() =
        runTest {
            every { søknadRepository.hentSøknaderFor(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentSoknader(request)

            assertEquals(emptyList<Søknad>(), response)
        }

    @Test
    fun `én søknad`() =
        runTest {
            val søknader =
                listOf(
                    Søknad(
                        UUID.randomUUID().toString(),
                        "12345",
                        "Skjemakode",
                        Søknad.SøknadsType.NySøknad,
                        Søknad.Kanal.Digital,
                        LocalDateTime.now(),
                    ),
                )
            every { søknadRepository.hentSøknaderFor(any(), any(), any(), any(), any(), any(), any()) } returns søknader

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentSoknader(request)

            assertEquals(søknader, response)
        }

    @Test
    fun `flere søknader`() =
        runTest {
            val søknader =
                listOf(
                    Søknad(
                        UUID.randomUUID().toString(),
                        "11111",
                        "Skjemakode1",
                        Søknad.SøknadsType.NySøknad,
                        Søknad.Kanal.Digital,
                        LocalDateTime.now(),
                    ),
                    Søknad(
                        UUID.randomUUID().toString(),
                        "2222",
                        "Skjemakode2",
                        Søknad.SøknadsType.Gjenopptak,
                        Søknad.Kanal.Papir,
                        LocalDateTime.now(),
                    ),
                )
            every { søknadRepository.hentSøknaderFor(any(), any(), any(), any(), any(), any(), any()) } returns søknader

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentSoknader(request)

            assertEquals(søknader, response)
        }

    @Test
    fun `ingen vedtak`() =
        runTest {
            every { vedtakRepository.hentVedtakFor(any(), any(), any(), any(), any(), any()) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentVedtak(request)

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
                        Vedtak.Status.INNVILGET,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                    ),
                )
            every { vedtakRepository.hentVedtakFor(any(), any(), any(), any(), any(), any()) } returns vedtak

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentVedtak(request)

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
                        Vedtak.Status.INNVILGET,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                    ),
                    Vedtak(
                        "VedtakId2",
                        "FagsakId2",
                        Vedtak.Status.AVSLÅTT,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                    ),
                )
            every { vedtakRepository.hentVedtakFor(any(), any(), any(), any(), any(), any()) } returns vedtak

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = innsynService.hentVedtak(request)

            assertEquals(vedtak, response)
        }
}
