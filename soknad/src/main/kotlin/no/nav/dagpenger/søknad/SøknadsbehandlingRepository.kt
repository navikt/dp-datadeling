package no.nav.dagpenger.søknad

import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import java.time.LocalDateTime
import java.util.UUID

/**
 * Skriveflate for tilstanden til søknadsbehandlinger (leses via SøknadBehandlingGraf), uavhengig av søknadene som lagres fra innsending_ferdigstilt.
 * Hendelsene kan komme i vilkårlig rekkefølge, så begge metodene er idempotente upserts.
 */
interface SøknadsbehandlingRepository {
    /** Lagrer søknaden som UnderBehandling hvis den er ukjent. Kjent tilstand overskrives aldri. */
    fun lagreOpprettet(
        søknadId: UUID,
        behandlingId: UUID,
        sakId: UUID,
        ident: String,
        opprettet: LocalDateTime,
    )

    /** Setter tilstanden (og utfall, førteTil fra ferdig), og oppretter raden hvis søknaden er ukjent. Eldre hendelser overskriver ikke nyere. */
    fun lagreTilstand(
        søknadId: UUID,
        behandlingId: UUID,
        sakId: UUID,
        ident: String,
        tilstand: SøknadsbehandlingTilstand,
        tidspunkt: LocalDateTime,
        utfall: String? = null,
    )
}
