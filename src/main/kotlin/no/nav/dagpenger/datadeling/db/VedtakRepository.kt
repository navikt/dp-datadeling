package no.nav.dagpenger.datadeling.db

import kotliquery.Row
import kotliquery.param
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
    fun lagreVedtak(
        id: String,
        ident: String,
        fagsakId: String,
        utfall: Utfall,
        stønadType: StønadTypeDagpenger,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate? = null,
        dagsats: Int? = null,
        barnetillegg: Int? = null,
        kilde: Kilde,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """INSERT INTO vedtak (id, ident, sak_id, utfall, stonad_type, fra_og_med_dato, til_og_med_dato, dagsats, barnetillegg, kilde)
                        VALUES (:ident, :vedtakId, :fagsakId, :utfall, :stonadType, :fraOgMedDato, :tilOgMedDato, :dagsats, :barnetillegg, :kilde)
                        ON CONFLICT DO NOTHING
                """.trimMargin(),
                mapOf(
                    "id" to id,
                    "ident" to ident,
                    "sak_id" to fagsakId,
                    "utfall" to utfall.toString(),
                    "stonad_type" to stønadType.toString(),
                    "fra_og_med_dato" to fraOgMedDato,
                    "til_og_med_dato" to tilOgMedDato,
                    "dagsats" to dagsats,
                    "barnetillegg" to barnetillegg,
                    "kilde" to kilde.toString(),
                ),
            ).asUpdate,
        )
    }

    fun hentVedtakFor(
        ident: String,
        fom: LocalDate? = null,
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
                  AND s.dato_innsendt <@ TSRANGE(:fom, :tom) 
                ORDER BY s.dato_innsendt DESC
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
                mapOf(
                    "ident" to ident.param(),
                    "fom" to fom,
                    "tom" to tom?.plusDays(1),
                    "limit" to limit,
                    "offset" to offset,
                ),
            ).map { row -> row.toVedtak() }.asList,
        )
    }

    fun hentSisteVedtak(ident: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT *
                    FROM vedtak s
                    WHERE s.ident = :ident
                    ORDER BY s.fra_og_med_dato DESC
                    LIMIT 1
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident.param(),
                    ),
                ).map { row -> row.toVedtak() }.asSingle,
            )
        }

    private fun Row.toVedtak() =
        Vedtak(
            vedtakId = string("id"),
            fagsakId = string("fagsak_id"),
            utfall = Utfall.valueOf(string("utfall")),
            stønadType = StønadTypeDagpenger.valueOf(string("stonad_type")),
            fraOgMedDato = localDate("fra_og_med_dato"),
            tilOgMedDato = localDateOrNull("til_og_med_dato"),
            dagsats = intOrNull("dagsats"),
            barnetillegg = intOrNull("barnetillegg"),
            kilde = Kilde.valueOf(string("kilde")),
        )
}
