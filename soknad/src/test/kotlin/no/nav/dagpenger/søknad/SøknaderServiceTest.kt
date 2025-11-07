package no.nav.dagpenger.søknad

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.søknad.modell.Søknad
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class SøknaderServiceTest {
    private val søknadRepository = mockk<SøknadRepository>()
    private val søknadService = SøknadService(søknadRepository)
    private val ident = "01020312345"

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `hentSøknader returnerer en tom liste når det ikke finnes søknader`() {
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

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = søknadService.hentSøknader(request)

        assertEquals(emptyList<Søknad>(), response)
    }

    @Test
    fun `hentSøknader returnerer en liste med ett element når det finnes én søknad`() {
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
        coEvery { søknadRepository.hentSøknaderFor(any(), any(), any(), any(), any(), any(), any()) } returns søknader

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = søknadService.hentSøknader(request)

        assertEquals(søknader, response)
    }

    @Test
    fun `hentSøknader returnerer en liste med flere søknader`() {
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

        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))
        val response = søknadService.hentSøknader(request)

        assertEquals(søknader, response)
    }

    @Test
    fun `hentSisteSøknad returnerer null når det ikke finnes søknader`() {
        every { søknadRepository.hentSisteSøknad(any()) } returns null

        val sisteSoknad = søknadService.hentSisteSøknad(ident)
        assertEquals(null, sisteSoknad)
    }

    @Test
    fun `hentSisteSøknad returnerer søknad når den finnes`() {
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

        val sisteSoknad = søknadService.hentSisteSøknad(ident)
        assertEquals(søknad, sisteSoknad)
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
