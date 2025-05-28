package no.nav.dagpenger.datadeling.db

import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import no.nav.dagpenger.datadeling.model.Vedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class VedtakRepositoryTest {
    private val repository = VedtakRepository()

    @Test
    fun `Lagring av vedtak er idempotent`() {
        withMigratedDb {
            val ident = "01020312345"
            val vedtak =
                Vedtak(
                    "123",
                    "fagsakid",
                    Vedtak.Status.INNVILGET,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                )

            repository.lagreVedtak(
                ident,
                vedtak.vedtakId,
                vedtak.fagsakId,
                vedtak.status,
                vedtak.datoFattet,
                vedtak.fraDato,
                vedtak.tilDato,
            )

            assertDoesNotThrow {
                repository
                    .hentVedtakFor(
                        ident = ident,
                        fattetFom = null,
                        fattetTom = null,
                    ).also {
                        assertEquals(1, it.size)
                    }
                repository
                    .hentVedtakFor(
                        ident = ident,
                        fattetFom = LocalDate.now().minusDays(2),
                        fattetTom = null,
                    ).also {
                        assertEquals(1, it.size)
                    }
                repository
                    .hentVedtakFor(
                        ident = ident,
                        fattetFom = LocalDate.of(2021, 4, 30),
                        fattetTom = LocalDate.of(2021, 5, 30),
                    ).also {
                        assertEquals(0, it.size)
                    }
                repository
                    .hentVedtakFor(
                        ident = ident,
                        fattetFom = null,
                        fattetTom = LocalDate.now().plusDays(2),
                    ).also {
                        assertEquals(1, it.size)
                    }
            }
        }
    }

    @Test
    fun `Lagret vedtak kan oppdateres`() {
        withMigratedDb {
            val ident = "01020312345"
            val vedtak =
                Vedtak(
                    "123",
                    "fagsakid",
                    Vedtak.Status.INNVILGET,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                )

            repository.lagreVedtak(
                ident,
                vedtak.vedtakId,
                vedtak.fagsakId,
                vedtak.status,
                vedtak.datoFattet,
                vedtak.fraDato,
                vedtak.tilDato,
            )

            repository
                .hentVedtakFor(
                    ident = ident,
                    fattetFom = null,
                    fattetTom = null,
                ).also {
                    assertEquals(null, it[0].tilDato)
                }

            val tilDato = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
            repository.lagreVedtak(
                ident,
                vedtak.vedtakId,
                vedtak.fagsakId,
                vedtak.status,
                vedtak.datoFattet,
                vedtak.fraDato,
                tilDato,
            )

            repository
                .hentVedtakFor(
                    ident = ident,
                    fattetFom = null,
                    fattetTom = null,
                ).also {
                    assertEquals(tilDato, it[0].tilDato)
                }
        }
    }
}
