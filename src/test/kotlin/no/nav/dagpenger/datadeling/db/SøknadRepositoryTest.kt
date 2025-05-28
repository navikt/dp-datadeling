package no.nav.dagpenger.datadeling.db

import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import no.nav.dagpenger.datadeling.model.Søknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class SøknadRepositoryTest {
    private val repository = SøknadRepository()

    @Test
    fun `skal lagre og finne søknad`() {
        withMigratedDb {
            val ident = "01020312345"
            val søknad =
                Søknad(
                    UUID.randomUUID().toString(),
                    "journalpostId",
                    "NAV01",
                    Søknad.SøknadsType.NySøknad,
                    Søknad.Kanal.Digital,
                    LocalDateTime.now(),
                )

            repository.lagreSøknad(
                ident,
                søknad.søknadId,
                søknad.journalpostId,
                søknad.skjemaKode,
                søknad.søknadsType,
                søknad.kanal,
                søknad.datoInnsendt,
            )

            repository.hentSøknaderFor(ident = ident, fom = LocalDate.now().minusDays(30)).also {
                assertEquals(1, it.size)
            }
            repository
                .hentSøknaderFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(30),
                    tom = LocalDate.now().plusDays(0),
                ).also {
                    assertEquals(1, it.size)
                }
            repository.hentSøknaderFor(ident = ident, tom = LocalDate.now().plusDays(2)).also {
                assertEquals(1, it.size)
            }
        }
    }

    @Test
    fun `skal finne papirsøknad for person`() {
        withMigratedDb {
            val ident = "01020312345"
            val søknad =
                Søknad(
                    null,
                    "journalpostId",
                    "NAV01",
                    Søknad.SøknadsType.NySøknad,
                    Søknad.Kanal.Papir,
                    LocalDateTime.now(),
                )

            repository.lagreSøknad(
                ident,
                søknad.søknadId,
                søknad.journalpostId,
                søknad.skjemaKode,
                søknad.søknadsType,
                søknad.kanal,
                søknad.datoInnsendt,
            )

            repository.hentSøknaderFor(ident, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)).also {
                assertFalse(it.isEmpty())
                assertSøknadEquals(søknad, it.first())
            }
        }
    }

    private fun assertSøknadEquals(
        expected: Søknad,
        result: Søknad,
    ) {
        assertEquals(
            expected.datoInnsendt.truncatedTo(ChronoUnit.SECONDS),
            result.datoInnsendt.truncatedTo(ChronoUnit.SECONDS),
        )
        assertEquals(expected.journalpostId, result.journalpostId)
        assertEquals(expected.kanal, result.kanal)
        assertEquals(expected.skjemaKode, result.skjemaKode)
        assertEquals(expected.søknadId, result.søknadId)
        assertEquals(expected.søknadsType, result.søknadsType)
    }
}
