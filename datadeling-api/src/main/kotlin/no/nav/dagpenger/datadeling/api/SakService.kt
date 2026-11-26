package no.nav.dagpenger.datadeling.api

import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.SakStatus
import no.nav.dagpenger.behandling.overlapper
import no.nav.dagpenger.behandling.tilDTO
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.GjenstaaendeDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.RettighetsperiodeDTO
import no.nav.dagpenger.datadeling.models.SakAvsluttetDTO
import no.nav.dagpenger.datadeling.models.SakDTO
import no.nav.dagpenger.datadeling.models.SakInkluderDTO
import no.nav.dagpenger.datadeling.models.SakLoependeDTO
import no.nav.dagpenger.datadeling.models.SakTilBehandlingDTO
import no.nav.dagpenger.datadeling.models.SoknadDTO
import no.nav.dagpenger.datadeling.models.SoknadTilstandDTO
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.søknad.SøknadBehandlingGraf
import no.nav.dagpenger.søknad.modell.Søknadsbehandling
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import no.nav.dagpenger.søknad.modell.sistePerSøknad
import java.time.LocalDate
import java.util.UUID

/**
 * En sak er en behandlingskjede (sakId). Både behandlingsresultat og søknadsbehandling_opprettet åpner en sak, og alle saker vises.
 * `soknader` fylles med alle søknadsbehandlinger i saken (nyeste først) når det finnes, ellers utelates feltet. innsendt er soknad.dato_innsendt
 * (innsending_ferdigstilt), eller @opprettet fra søknadsbehandling_opprettet hvis innsendingen ikke er registrert. Tilstand
 * kommer fra soknadsbehandling, og utfall er førteTil fra søknadsbehandling_ferdig. En søknad uten soknadsbehandling-rad er
 * Innsendt, men har ikke sak_id og vises derfor ikke i sak-lister.
 *
 * Status: gjeldende eller åpen harRett-periode i siste behandling gir Løpende, og når alle harRett-perioder er slutt gir
 * det Avsluttet (se Sak). Alt annet er TilBehandling: ingen behandlingsresultat ennå, avslag, bare fremtidige perioder,
 * eller avbrutte/trukne søknadsbehandlinger.
 */
class SakService(
    private val behandlinger: BehandlingResultatRepositoryMedTolker,
    private val meldekortService: MeldekortService,
    private val søknadBehandlingGraf: SøknadBehandlingGraf,
) {
    suspend fun hentSaker(
        request: DatadelingRequestDTO,
        inkluder: Set<SakInkluderDTO>,
    ): List<SakDTO> {
        val saker = behandlinger.hentSaker(request.personIdent)
        val søknadsbehandlingerPerSak =
            søknadBehandlingGraf
                .søknaderForPerson(request.personIdent)
                .flatMap { it.søknadsbehandlinger }
                .filter {
                    it.sakId != null
                }.groupBy { it.sakId!! }

        val medResultat = saker.map { it.tilDTO(inkluder, søknadsbehandlingerPerSak[it.sakId].orEmpty()) }
        val barePåSøknad =
            søknadsbehandlingerPerSak
                .filterKeys { sakId -> saker.none { it.sakId == sakId } }
                .map { (sakId, søknadsbehandlinger) -> sakUtenResultat(sakId, søknadsbehandlinger, inkluder) }
        return medResultat + barePåSøknad
    }

    suspend fun hentSak(
        sakId: UUID,
        inkluder: Set<SakInkluderDTO>,
    ): SakDTO? {
        val søknadsbehandlinger = søknadBehandlingGraf.søknaderForSak(sakId).flatMap { it.søknadsbehandlinger }
        return behandlinger.hentSak(sakId)?.tilDTO(inkluder, søknadsbehandlinger)
            ?: søknadsbehandlinger.takeIf { it.isNotEmpty() }?.let { sakUtenResultat(sakId, it, inkluder) }
    }

    private fun List<Søknadsbehandling>.tilDTO() =
        sistePerSøknad().sortedByDescending { it.innsendt }.map {
            SoknadDTO(
                soknadId = it.søknadId,
                innsendt = it.innsendt,
                tilstand = it.tilstand.tilDTO(),
                utfall = it.utfall,
            )
        }

    private fun SøknadsbehandlingTilstand.tilDTO() =
        when (this) {
            SøknadsbehandlingTilstand.Innsendt -> SoknadTilstandDTO.INNSENDT
            SøknadsbehandlingTilstand.UnderBehandling -> SoknadTilstandDTO.UNDER_BEHANDLING
            SøknadsbehandlingTilstand.Ferdig -> SoknadTilstandDTO.FERDIG
            SøknadsbehandlingTilstand.Avbrutt -> SoknadTilstandDTO.AVBRUTT
        }

    private fun sakUtenResultat(
        sakId: UUID,
        søknadsbehandlinger: List<Søknadsbehandling>,
        inkluder: Set<SakInkluderDTO>,
    ) = SakTilBehandlingDTO(
        sakId = sakId,
        soknader = søknadsbehandlinger.tilDTO(),
        meldekort = if (SakInkluderDTO.MELDEKORT in inkluder) emptyList() else null,
    )

    private suspend fun Sak.tilDTO(
        inkluder: Set<SakInkluderDTO>,
        søknadsbehandlinger: List<Søknadsbehandling>,
    ): SakDTO {
        val soknader = søknadsbehandlinger.takeIf { it.isNotEmpty() }?.tilDTO()
        val meldekort = if (SakInkluderDTO.MELDEKORT in inkluder) hentMeldekort() else null
        val beregninger = if (SakInkluderDTO.BEREGNINGER in inkluder) beregninger.map { it.tilDTO() } else null

        return when (status()) {
            SakStatus.TIL_BEHANDLING -> {
                SakTilBehandlingDTO(sakId = sakId, soknader = soknader, meldekort = meldekort)
            }

            SakStatus.LOEPENDE -> {
                SakLoependeDTO(
                    sakId = sakId,
                    soknader = soknader,
                    meldekort = meldekort,
                    beregninger = beregninger,
                    rettighetsperioder = rettighetsperioderDTO(),
                    gjenstående = gjenstående(),
                    sistEndret = sistEndret,
                )
            }

            SakStatus.AVSLUTTET -> {
                SakAvsluttetDTO(
                    sakId = sakId,
                    soknader = soknader,
                    meldekort = meldekort,
                    beregninger = beregninger,
                    rettighetsperioder = rettighetsperioderDTO(),
                    sistEndret = sistEndret,
                )
            }
        }
    }

    private fun Sak.gjenstående(): GjenstaaendeDTO? =
        listOf(gjenståendeDager, gjenståendeSanksjon, gjenståendeTidsbegrensetBortfall)
            .takeIf { alle -> alle.any { it != null } }
            ?.let {
                GjenstaaendeDTO(
                    dager = gjenståendeDager,
                    sanksjon = gjenståendeSanksjon,
                    tidsbegrensetBortfall = gjenståendeTidsbegrensetBortfall,
                )
            }

    private fun Sak.rettighetsperioderDTO() =
        rettighetsperioder.map { RettighetsperiodeDTO(fraOgMed = it.fraOgMed, tilOgMed = it.tilOgMed, harRett = it.harRett) }

    // Meldekort har ingen sakstilknytning, så de avgrenses til overlapp med sakens periode
    private suspend fun Sak.hentMeldekort(): List<MeldekortDTO> {
        val fraOgMed = fraOgMed ?: return emptyList()
        val sakensPeriode = fraOgMed..(tilOgMed ?: LocalDate.MAX)
        return meldekortService
            .hentMeldekort(DatadelingRequestDTO(personIdent = ident, fraOgMedDato = fraOgMed, tilOgMedDato = tilOgMed))
            .filter { (it.periode.fraOgMed..it.periode.tilOgMed) overlapper sakensPeriode }
            .sortedByDescending { it.periode.fraOgMed }
    }
}
