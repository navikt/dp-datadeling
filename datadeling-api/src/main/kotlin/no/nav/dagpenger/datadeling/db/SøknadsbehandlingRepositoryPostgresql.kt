package no.nav.dagpenger.datadeling.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.søknad.SøknadsbehandlingRepository
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import java.time.LocalDateTime
import java.util.UUID

class SøknadsbehandlingRepositoryPostgresql : SøknadsbehandlingRepository {
    override fun lagreOpprettet(
        søknadId: UUID,
        behandlingId: UUID,
        sakId: UUID,
        ident: String,
        opprettet: LocalDateTime,
    ) = upsert(
        //language=PostgreSQL
        """
        INSERT INTO soknadsbehandling (soknad_id, behandling_id, sak_id, ident, opprettet, tilstand, sist_endret, utfall)
        VALUES (:soknadId, :behandlingId, :sakId, :ident, :tidspunkt, :tilstand, :tidspunkt, :utfall)
        ON CONFLICT (behandling_id) DO UPDATE SET opprettet = LEAST(soknadsbehandling.opprettet, EXCLUDED.opprettet)
        """.trimIndent(),
        søknadId,
        behandlingId,
        sakId,
        ident,
        SøknadsbehandlingTilstand.UnderBehandling,
        opprettet,
        null,
    )

    override fun lagreTilstand(
        søknadId: UUID,
        behandlingId: UUID,
        sakId: UUID,
        ident: String,
        tilstand: SøknadsbehandlingTilstand,
        tidspunkt: LocalDateTime,
        utfall: String?,
    ) = upsert(
        //language=PostgreSQL
        """
        INSERT INTO soknadsbehandling (soknad_id, behandling_id, sak_id, ident, opprettet, tilstand, sist_endret, utfall)
        VALUES (:soknadId, :behandlingId, :sakId, :ident, :tidspunkt, :tilstand, :tidspunkt, :utfall)
        ON CONFLICT (behandling_id) DO UPDATE SET
            tilstand = EXCLUDED.tilstand,
            utfall = EXCLUDED.utfall,
            sist_endret = EXCLUDED.sist_endret
        WHERE soknadsbehandling.sist_endret <= EXCLUDED.sist_endret
        """.trimIndent(),
        søknadId,
        behandlingId,
        sakId,
        ident,
        tilstand,
        tidspunkt,
        utfall,
    )

    private fun upsert(
        sql: String,
        søknadId: UUID,
        behandlingId: UUID,
        sakId: UUID,
        ident: String,
        tilstand: SøknadsbehandlingTilstand,
        tidspunkt: LocalDateTime,
        utfall: String?,
    ) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    sql,
                    mapOf(
                        "soknadId" to søknadId,
                        "behandlingId" to behandlingId,
                        "sakId" to sakId,
                        "ident" to ident,
                        "tilstand" to tilstand.name,
                        "tidspunkt" to tidspunkt,
                        "utfall" to utfall,
                    ),
                ).asUpdate,
            )
        }
    }
}
