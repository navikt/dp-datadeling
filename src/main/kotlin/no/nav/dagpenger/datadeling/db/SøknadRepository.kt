package no.nav.dagpenger.datadeling.db

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.model.Søknad
import java.time.LocalDate
import java.time.LocalDateTime

class SøknadRepository {
    fun lagreSøknad(
        ident: String,
        søknadId: String?,
        journalpostId: String,
        skjemaKode: String?,
        søknadsType: Søknad.SøknadsType,
        kanal: Søknad.Kanal,
        datoInnsendt: LocalDateTime,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """INSERT INTO soknad (ident, soknad_id, journalpost_id, skjema_kode, soknads_type, kanal, dato_innsendt)
                        VALUES (:ident, :soknadId, :journalpostId, :skjemaKode, :soknadsType, :kanal, :datoInnsendt)
                        ON CONFLICT DO NOTHING
                """.trimMargin(),
                mapOf(
                    "ident" to ident,
                    "soknadId" to søknadId,
                    "journalpostId" to journalpostId,
                    "skjemaKode" to skjemaKode,
                    "soknadsType" to søknadsType.toString(),
                    "kanal" to kanal.toString(),
                    "datoInnsendt" to datoInnsendt,
                ),
            ).asUpdate,
        )
    }

    fun hentSøknaderFor(
        ident: String,
        fom: LocalDate? = null,
        tom: LocalDate? = null,
        type: List<Søknad.SøknadsType> = emptyList(),
        kanal: List<Søknad.Kanal> = emptyList(),
        offset: Int = 0,
        limit: Int = 20,
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT *
                FROM soknad s
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
            ).map { row -> row.toSøknad() }.asList,
        )
    }

    fun hentSisteSøknad(ident: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT *
                    FROM soknad s
                    WHERE s.ident = :ident
                    ORDER BY s.dato_innsendt DESC
                    LIMIT 1
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident.param(),
                    ),
                ).map { row -> row.toSøknad() }.asSingle,
            )
        }

    private fun Row.toSøknad() =
        Søknad(
            søknadId = stringOrNull("soknad_id"),
            journalpostId = string("journalpost_id"),
            skjemaKode = string("skjema_kode"),
            søknadsType = Søknad.SøknadsType.valueOf(string("soknads_type")),
            kanal = Søknad.Kanal.valueOf(string("kanal")),
            datoInnsendt = localDateTime("dato_innsendt"),
        )
}
