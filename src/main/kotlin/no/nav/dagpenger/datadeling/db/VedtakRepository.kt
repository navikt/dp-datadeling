package no.nav.dagpenger.datadeling.db

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.model.Vedtak
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRepository {
    fun lagreVedtak(
        ident: String,
        vedtakId: String,
        fagsakId: String,
        status: Vedtak.Status,
        datoFattet: LocalDateTime,
        fraDato: LocalDateTime,
        tilDato: LocalDateTime?,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """INSERT INTO vedtak (ident, vedtak_id, fagsak_id, status, fattet, fra_dato, til_dato)
                        VALUES (:ident, :vedtakId, :fagsakId, :status, :fattet, :fraDato, :tilDato)
                        ON CONFLICT (vedtak_id) DO UPDATE SET (ident, fagsak_id, status, fattet, fra_dato, til_dato) = (
                            excluded.ident, excluded.fagsak_id, excluded.status, excluded.fattet, excluded.fra_dato, excluded.til_dato
                        )
                """.trimMargin(),
                mapOf(
                    "ident" to ident,
                    "vedtakId" to vedtakId,
                    "fagsakId" to fagsakId,
                    "status" to status.toString(),
                    "fattet" to datoFattet,
                    "fraDato" to fraDato,
                    "tilDato" to tilDato,
                ),
            ).asUpdate,
        )
    }

    fun hentVedtakFor(
        ident: String,
        fattetFom: LocalDate? = null,
        fattetTom: LocalDate? = null,
        status: List<Vedtak.Status> = emptyList(),
        offset: Int = 0,
        limit: Int = 20,
    ): List<Vedtak> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT *
                    FROM vedtak v
                    WHERE v.ident = :ident
                      AND v.fattet <@ TSRANGE(:fom, :tom) 
                    ORDER BY v.fattet DESC
                    LIMIT :limit OFFSET :offset
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                        "fom" to fattetFom,
                        "tom" to fattetTom?.plusDays(1),
                        "limit" to limit,
                        "offset" to offset,
                    ),
                ).map { row -> row.toVedtak() }.asList,
            )
        }

    private fun Row.toVedtak() =
        Vedtak(
            vedtakId = string("vedtak_id"),
            fagsakId = string("fagsak_id"),
            status = Vedtak.Status.valueOf(string("status")),
            datoFattet = localDateTime("fattet"),
            fraDato = localDateTime("fra_dato"),
            tilDato = localDateTimeOrNull("til_dato"),
        )
}
