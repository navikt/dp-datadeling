package no.nav.dagpenger.datadeling.db

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.model.Vedtak.Kilde
import no.nav.dagpenger.datadeling.model.Vedtak.Utfall
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger
import java.time.LocalDate

class VedtakRepository {
    fun slettAlleVedtakForSak(sakId: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "DELETE FROM vedtak where sak_id = :sakId",
                    mapOf(
                        "sakId" to sakId,
                    ),
                ).asUpdate,
            )
        }

    fun lagreVedtak(
        vedtakId: String,
        sakId: String,
        ident: String,
        utfall: Utfall,
        stønadType: StønadTypeDagpenger,
        kilde: Kilde,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate? = null,
        dagsats: Int? = null,
        barnetillegg: Int? = null,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """INSERT INTO vedtak (vedtak_id, sak_id, ident, utfall, stonad_type, fra_og_med_dato, til_og_med_dato, dagsats, barnetillegg, kilde)
                        VALUES (:vedtakId, :sakId, :ident, :utfall, :stonadType, :fraOgMedDato, :tilOgMedDato, :dagsats, :barnetillegg, :kilde)
                        ON CONFLICT DO NOTHING
                """.trimMargin(),
                mapOf(
                    "vedtakId" to vedtakId,
                    "sakId" to sakId,
                    "ident" to ident,
                    "utfall" to utfall.toString(),
                    "stonadType" to stønadType.toString(),
                    "fraOgMedDato" to fraOgMedDato,
                    "tilOgMedDato" to tilOgMedDato,
                    "dagsats" to dagsats,
                    "barnetillegg" to barnetillegg,
                    "kilde" to kilde.toString(),
                ),
            ).asUpdate,
        )
    }

    fun hentVedtakFor(
        ident: String,
        fom: LocalDate,
        tom: LocalDate? = null,
        offset: Int = 0,
        limit: Int = 20,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT *
                FROM vedtak s
                WHERE s.ident = :ident
                  AND (
                    (s.fra_og_med_dato <= :fom AND s.til_og_med_dato >= :fom AND (cast(:tom AS TIMESTAMP) IS NULL))
                    OR
                    (s.fra_og_med_dato <= :fom AND s.til_og_med_dato IS NULL AND (cast(:tom AS TIMESTAMP) IS NULL))
                    OR
                    (s.fra_og_med_dato >= :fom AND s.til_og_med_dato IS NULL)
                    OR
                    (s.fra_og_med_dato >= :fom AND s.til_og_med_dato >= :fom)
                  )
                  AND (
                    (cast(:tom AS TIMESTAMP) IS NULL)
                    OR
                    (s.fra_og_med_dato <= :tom AND s.til_og_med_dato >= :tom)
                    OR
                    (s.fra_og_med_dato <= :tom AND s.til_og_med_dato IS NULL)
                  )
                ORDER BY s.fra_og_med_dato DESC
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
                mapOf(
                    "ident" to ident,
                    "fom" to fom,
                    "tom" to tom,
                    "limit" to limit,
                    "offset" to offset,
                ),
            ).map { row -> row.toVedtak() }.asList,
        )
    }

    private fun Row.toVedtak() =
        Vedtak(
            vedtakId = string("vedtak_id"),
            fagsakId = string("sak_id"),
            utfall = Utfall.valueOf(string("utfall")),
            stønadType = StønadTypeDagpenger.valueOf(string("stonad_type")),
            fraOgMedDato = localDate("fra_og_med_dato"),
            tilOgMedDato = localDateOrNull("til_og_med_dato"),
            dagsats = intOrNull("dagsats"),
            barnetillegg = intOrNull("barnetillegg"),
            kilde = Kilde.valueOf(string("kilde")),
        )
}
