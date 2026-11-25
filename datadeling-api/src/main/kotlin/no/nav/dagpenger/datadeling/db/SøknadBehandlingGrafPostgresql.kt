package no.nav.dagpenger.datadeling.db

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.SakBehandling
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.søknad.SøknadBehandlingGraf
import no.nav.dagpenger.søknad.modell.SøknadMedBehandlinger
import no.nav.dagpenger.søknad.modell.Søknadsbehandling
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import java.util.UUID

class SøknadBehandlingGrafPostgresql(
    private val behandlinger: BehandlingResultatRepositoryMedTolker,
) : SøknadBehandlingGraf {
    // soknad.soknad_id er tekst (og null/ikke-UUID for papir), så koblingen castes UUID -> tekst og aldri motsatt
    override fun søknaderForPerson(ident: String) =
        hent(
            "soknad s FULL JOIN soknadsbehandling b ON b.soknad_id::text = s.soknad_id",
            "s.ident = :param OR b.ident = :param",
            ident,
        )

    override fun søknaderForSak(sakId: UUID) =
        hent("soknadsbehandling b LEFT JOIN soknad s ON b.soknad_id::text = s.soknad_id", "b.sak_id = :param", sakId)

    override fun søknadForBehandling(behandlingId: UUID) =
        hent("soknadsbehandling b LEFT JOIN soknad s ON b.soknad_id::text = s.soknad_id", "b.behandling_id = :param", behandlingId)
            .singleOrNull()

    override fun resultaterForSøknad(søknadId: UUID): List<SakBehandling> =
        hent("soknadsbehandling b LEFT JOIN soknad s ON b.soknad_id::text = s.soknad_id", "b.soknad_id = :param", søknadId)
            .flatMap { it.søknadsbehandlinger }
            .mapNotNull { it.behandlingId }
            .mapNotNull { behandlinger.hentBehandling(it) }

    private fun hent(
        fra: String,
        betingelse: String,
        param: Any,
    ): List<SøknadMedBehandlinger> =
        sessionOf(dataSource)
            .use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT COALESCE(b.soknad_id::text, s.soknad_id) AS soknad_id, b.sak_id, b.behandling_id, b.tilstand, b.utfall, b.sist_endret,
                               COALESCE(s.dato_innsendt, b.opprettet) AS innsendt
                        FROM $fra
                        WHERE $betingelse
                        ORDER BY b.sist_endret
                        """.trimIndent(),
                        mapOf("param" to param),
                    ).map { it.toSøknadsbehandling() }.asList,
                )
            }.filterNotNull()
            .groupBy { it.søknadId }
            .map { (søknadId, søknadsbehandlinger) -> SøknadMedBehandlinger(søknadId, søknadsbehandlinger) }

    private fun Row.toSøknadsbehandling(): Søknadsbehandling? {
        val søknadId = runCatching { UUID.fromString(stringOrNull("soknad_id")) }.getOrNull() ?: return null
        val tilstand = stringOrNull("tilstand")?.let { SøknadsbehandlingTilstand.valueOf(it) } ?: SøknadsbehandlingTilstand.Innsendt
        return Søknadsbehandling(
            søknadId = søknadId,
            sakId = uuidOrNull("sak_id"),
            innsendt = localDateTime("innsendt"),
            tilstand = tilstand,
            utfall = stringOrNull("utfall").takeIf { tilstand == SøknadsbehandlingTilstand.Ferdig },
            behandlingId = uuidOrNull("behandling_id"),
            sistEndret = localDateTimeOrNull("sist_endret"),
        )
    }
}
