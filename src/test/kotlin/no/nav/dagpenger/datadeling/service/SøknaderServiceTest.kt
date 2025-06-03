package no.nav.dagpenger.datadeling.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.model.Søknad
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
    fun `ingen søknader`() =
        runTest {
            every { søknadRepository.hentSøknaderFor(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

            val request = enDatadelingRequest(1.januar()..10.januar())
            val response = søknaderService.hentSoknader(request)

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
            val response = søknaderService.hentSoknader(request)

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
            val response = søknaderService.hentSoknader(request)

            assertEquals(søknader, response)
        }
}
