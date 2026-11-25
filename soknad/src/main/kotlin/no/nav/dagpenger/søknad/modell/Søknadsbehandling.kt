package no.nav.dagpenger.søknad.modell

import java.time.LocalDateTime
import java.util.UUID

enum class SøknadsbehandlingTilstand {
    Innsendt,
    UnderBehandling,
    Ferdig,
    Avbrutt,
}

/**
 * En søknad slik NKS ser den: soknad (innsending_ferdigstilt) koblet med soknadsbehandling (tilstand etter innsending) på søknadId.
 * Uten søknadsbehandling er tilstanden Innsendt, og sakId og behandlingId er null (søknaden hører ikke til en sak ennå).
 * innsendt er tidspunktet søknaden ble innsendt, eller @opprettet fra søknadsbehandling_opprettet hvis innsendingen ikke er registrert.
 * behandlingId kobler mot behandlingsresultat.behandling_id, og sakId (= behandlingsskjedeId) grupperer behandlinger i en sak.
 * utfall (førteTil) er bare satt når tilstanden er Ferdig. sistEndret er tidspunktet for siste tilstandsendring (null for Innsendt).
 */
data class Søknadsbehandling(
    val søknadId: UUID,
    val sakId: UUID?,
    val innsendt: LocalDateTime,
    val tilstand: SøknadsbehandlingTilstand,
    val utfall: String? = null,
    val behandlingId: UUID? = null,
    val sistEndret: LocalDateTime? = null,
)

/** En søknad med alle behandlinger av den (1—*). */
data class SøknadMedBehandlinger(
    val søknadId: UUID,
    val søknadsbehandlinger: List<Søknadsbehandling>,
)

/** En søknad kan ha flere behandlinger. Per søknad beholdes den sist endrede, som bestemmer søknadens tilstand. */
fun List<Søknadsbehandling>.sistePerSøknad(): List<Søknadsbehandling> =
    groupBy { it.søknadId }.values.map { behandlinger -> behandlinger.maxBy { it.sistEndret ?: LocalDateTime.MIN } }
