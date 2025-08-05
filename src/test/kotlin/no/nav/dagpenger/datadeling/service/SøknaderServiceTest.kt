package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SøknaderServiceTest {
    private val søknadRepository = mockk<SøknadRepository>()
    private val søknaderService = SøknaderService(søknadRepository)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `hentSøknader returnerer en tom liste når det ikke finnes søknader`() =
        runTest {
            every {
                søknadRepository.hentSøknaderFor(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = søknaderService.hentSøknader(request)

            assertEquals(emptyList<Søknad>(), response)
        }

    @Test
    fun `hentSøknader returnerer en liste med ett element når det finnes én søknad`() =
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
            val response = søknaderService.hentSøknader(request)

            assertEquals(søknader, response)
        }

    @Test
    fun `hentSøknader returnerer en liste med flere søknader`() =
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
            val response = søknaderService.hentSøknader(request)

            assertEquals(søknader, response)
        }

    @Test
    fun `hentSisteSøknad returnerer null når det ikke finnes søknader`() =
        runTest {
            every { søknadRepository.hentSisteSøknad(any()) } returns null

            val sisteSoknad = søknaderService.hentSisteSøknad(FNR)
            assertEquals(null, sisteSoknad)
        }

    @Test
    fun `hentSisteSøknad returnerer søknad når den finnes`() =
        runTest {
            val søknad =
                Søknad(
                    UUID.randomUUID().toString(),
                    "12345",
                    "Skjemakode",
                    Søknad.SøknadsType.NySøknad,
                    Søknad.Kanal.Digital,
                    LocalDateTime.now(),
                )
            every { søknadRepository.hentSisteSøknad(any()) } returns søknad

            val sisteSoknad = søknaderService.hentSisteSøknad(FNR)
            assertEquals(søknad, sisteSoknad)
        }
}
