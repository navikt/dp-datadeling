package no.nav.dagpenger.datadeling.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource

class BehandlingRepository {
    fun lagreData(
        behandlingId: String,
        søknadId: String,
        ident: String,
        sakId: String,
    ) = using(sessionOf(dataSource, true)) { session ->
        session
            .run(
                queryOf(
                    "INSERT INTO behandling " +
                        "(behandling_id, soknad_id, ident, sak_id) " +
                        "VALUES (?, ?, ?, ?)",
                    behandlingId,
                    søknadId,
                    ident,
                    sakId,
                ).asUpdate,
            )
    }.let { if (it == 0) throw Exception("Lagring av behandling feilet") }

    fun hentSisteSakId(ident: String): String? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT sak_id " +
                        "FROM behandling " +
                        "WHERE ident = ? " +
                        "ORDER BY tidspunkt DESC " +
                        "LIMIT 1",
                    ident,
                ).map {
                    it.string("sak_id")
                }.asSingle,
            )
        }

    fun hentSakIdForBehandlingId(behandlingId: String): String? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT sak_id " +
                        "FROM behandling " +
                        "WHERE behandling_id = ? " +
                        "LIMIT 1",
                    behandlingId,
                ).map {
                    it.string("sak_id")
                }.asSingle,
            )
        }
}
