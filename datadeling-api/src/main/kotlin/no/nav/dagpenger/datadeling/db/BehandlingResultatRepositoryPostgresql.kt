package no.nav.dagpenger.datadeling.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.BehandlingResultatRepository
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.objectMapper
import java.time.LocalDateTime
import java.util.UUID

class BehandlingResultatRepositoryPostgresql : BehandlingResultatRepository {
    override fun lagre(
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

    override fun oppdater(
        nyId: UUID,
        gammelId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                transaction.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        UPDATE behandlingresultat SET
                             behandling_id = :nyId,
                             json_data = :jsonData::jsonb,
                             opprettet = :opprettetTidspunkt
                        WHERE behandling_id = :gammelId
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
    }

    override fun ny(
        ident: String,
        behandlingId: UUID,
        sakId: UUID,
        json: String,
        opprettetTidspunkt: LocalDateTime,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                transaction.run(
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
    }

    override fun hent(ident: String): List<JsonNode> =
        sessionOf(dataSource).use { session ->
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
                    row.binaryStream("json_data").let { objectMapper.readTree(it) }
                }.asList,
            )
        }
}
