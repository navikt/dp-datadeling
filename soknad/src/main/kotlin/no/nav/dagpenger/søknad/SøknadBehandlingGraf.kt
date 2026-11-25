package no.nav.dagpenger.søknad

import no.nav.dagpenger.behandling.SakBehandling
import no.nav.dagpenger.søknad.modell.SøknadMedBehandlinger
import java.util.UUID

/**
 * Leseflate for koblingen søknad -> søknadsbehandling -> behandlingsresultat -> sak.
 * soknad-modulen avhenger av behandling-modulen (en søknad fører til behandling), ikke omvendt:
 * behandling kjenner ikke søknader, og koblingen skjer via behandlingId og sakId (= behandlingsskjedeId).
 */
interface SøknadBehandlingGraf {
    /** Alle personens søknader, også de uten søknadsbehandling (tilstand Innsendt). Papir/ikke-UUID søknadId utelates. */
    fun søknaderForPerson(ident: String): List<SøknadMedBehandlinger>

    /** Søknader som har en behandling i saken. */
    fun søknaderForSak(sakId: UUID): List<SøknadMedBehandlinger>

    /** Søknaden som en behandling (resultat) tilhører. */
    fun søknadForBehandling(behandlingId: UUID): SøknadMedBehandlinger?

    /** Behandlingsresultatene til søknadens behandlinger. Behandlinger uten resultat (ennå) er ikke med. */
    fun resultaterForSøknad(søknadId: UUID): List<SakBehandling>
}
