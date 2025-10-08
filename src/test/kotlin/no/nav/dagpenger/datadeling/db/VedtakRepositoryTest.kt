package no.nav.dagpenger.datadeling.db

import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VedtakRepositoryTest {
    private val repository = VedtakRepository()

    @Test
    fun `Skal lagre og finne vedtak uten TOM`() {
        withMigratedDb {
            val ident = "01020312345"
            val vedtakId = UUID.randomUUID().toString()
            val sakId = UUID.randomUUID().toString()

            repository.lagreVedtak(
                vedtakId,
                sakId,
                ident,
                Vedtak.Utfall.INNVILGET,
                StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                Vedtak.Kilde.DP,
                LocalDate.now(),
            )

            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(1),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now(),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().plusDays(1),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now().minusDays(1),
                ).also {
                    assertEquals(0, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now(),
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now().plusDays(5),
                ).also {
                    assertEquals(1, it.size)
                }
        }
    }

    @Test
    fun `Skal lagre og finne vedtak med TOM`() {
        withMigratedDb {
            val ident = "01020312345"
            val vedtakId = UUID.randomUUID().toString()
            val sakId = UUID.randomUUID().toString()

            repository.lagreVedtak(
                vedtakId,
                sakId,
                ident,
                Vedtak.Utfall.INNVILGET,
                StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                Vedtak.Kilde.DP,
                LocalDate.now(),
                LocalDate.now().plusDays(5),
            )

            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(1),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now(),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().plusDays(1),
                    tom = null,
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().plusDays(6),
                    tom = null,
                ).also {
                    assertEquals(0, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now().minusDays(1),
                ).also {
                    assertEquals(0, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now(),
                ).also {
                    assertEquals(1, it.size)
                }
            repository
                .hentVedtakFor(
                    ident = ident,
                    fom = LocalDate.now().minusDays(5),
                    tom = LocalDate.now().plusDays(5),
                ).also {
                    assertEquals(1, it.size)
                }
        }
    }
}
