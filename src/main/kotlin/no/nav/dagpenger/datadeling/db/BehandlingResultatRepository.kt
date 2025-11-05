package no.nav.dagpenger.datadeling.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.tjenester.BehandlingResultatV1Tolker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BehandlingResultatRepository {
    fun lagre(
        ident: String,
        behandlingId: UUID,
        basertPåId: UUID?,
        sakId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    ) {
        if (basertPåId != null) {
            oppdater(
                nyId = behandlingId,
                gammelId = basertPåId,
                json = json,
                opprettetTidspunkt = opprettetTidspunkt,
            )
        } else {
            ny(
                ident = ident,
                behandlingId = behandlingId,
                sakId = sakId,
                json = json,
                opprettetTidspunkt = opprettetTidspunkt,
            )
        }
    }

    fun oppdater(
        nyId: UUID,
        gammelId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    ) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    update behandlingresultat set
                         behandling_id = :nyId,
                         json_data = :jsonData::jsonb,
                         opprettet = :opprettetTidspunkt
                    where behandling_id = :gammelId
                    """.trimIndent(),
                    mapOf(
                        "nyId" to nyId,
                        "gammelId" to gammelId,
                        "jsonData" to json,
                        "opprettetTidspunkt" to opprettetTidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    fun ny(
        ident: String,
        behandlingId: UUID,
        sakId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    ) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO behandlingresultat (
                        ident, 
                        behandling_id, 
                        sak_id, 
                        json_data, 
                        opprettet
                    )
                    VALUES (
                        :ident, 
                        :behandlingId, 
                        :sakId, 
                        :jsonData::jsonb, 
                        :opprettetTidspunkt
                    )
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                        "behandlingId" to behandlingId,
                        "sakId" to sakId,
                        "jsonData" to json,
                        "opprettetTidspunkt" to opprettetTidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    fun hent(ident: String): List<BehandlingResultat> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT *
                    FROM behandlingresultat
                    WHERE ident = :ident
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                    ),
                ).map { row ->
                    row.toBehandlingResultat()
                }.asList,
            )
        }
}

fun Row.toBehandlingResultat(): BehandlingResultat {
    val jsonData = this.string("json_data")
    val behandlingResultatNode: JsonNode = objectMapper.readTree(jsonData)
    return BehandlingResultatV1Tolker(behandlingResultatNode)
}

interface BehandlingResultat {
    val ident: String
    val behandlingId: UUID
    val rettighetsperioder: List<Rettighetsperiode>
    val rettighetstyper: List<Rettighetstyper>
}

interface Rettighetsperiode {
    val fraOgMed: LocalDate
    val tilOgMed: LocalDate?
    val harRett: Boolean
}

interface Rettighetstyper {
    val type: Rettighetstype
    val fraOgMed: LocalDate
    val tilOgMed: LocalDate?
}

enum class Rettighetstype {
    ORDINÆR,
    PERMITTERING,
    LØNNSGARANTI,
    FISK,
}
