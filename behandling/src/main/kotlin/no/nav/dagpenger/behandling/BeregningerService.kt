package no.nav.dagpenger.behandling

import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.models.BeregnetDagDTO
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO
import java.time.LocalDate

/**
 * Service for å hente beregninger (utbetalingsdetaljer) fra både Arena og dp-sak.
 * Kombinerer data fra begge kilder til én samlet liste.
 */
class BeregningerService(
    private val arenaClient: ProxyClientArena,
    private val dpSakRepository: BehandlingResultatRepositoryMedTolker,
) {
    suspend fun hentBeregninger(request: DatadelingRequestDTO): List<BeregnetDagDTO> =
        coroutineScope {
            val arenaBeregninger = hentArenaBeregninger(request)
            val dpSakBeregninger = hentDpSakBeregninger(request.personIdent)

            val dager =
                (arenaBeregninger + dpSakBeregninger)
                    .sortedBy { it.fraOgMed }

            val ønsketPeriode = request.periode

            // Fjern alle beregnetDag med perioder som ikke inkluderes av forespørselen
            dager.filter { it.periode overlapper ønsketPeriode }
        }

    private val DatadelingRequestDTO.periode get() = fraOgMedDato..(tilOgMedDato ?: LocalDate.MAX)
    private val BeregnetDagDTO.periode get() = fraOgMed..tilOgMed

    private suspend fun hentArenaBeregninger(request: DatadelingRequestDTO): List<BeregnetDagDTO> =
        arenaClient.hentBeregninger(request).map { arenaBeregning ->
            BeregnetDagDTO(
                fraOgMed = arenaBeregning.meldekortFraDato,
                tilOgMed = arenaBeregning.meldekortTilDato,
                sats = arenaBeregning.innvilgetSats.toInt(),
                utbetaltBeløp = arenaBeregning.belop.toInt(),
                gjenståendeDager = arenaBeregning.antallDagerGjenstående.toInt(),
                kilde = FagsystemDTO.ARENA,
            )
        }

    private fun hentDpSakBeregninger(ident: String): List<BeregnetDagDTO> =
        dpSakRepository.hent(ident).flatMap { behandling ->
            behandling.beregninger.map { beregning ->
                BeregnetDagDTO(
                    fraOgMed = beregning.dato,
                    tilOgMed = beregning.dato,
                    sats = beregning.sats,
                    utbetaltBeløp = beregning.utbetaling,
                    gjenståendeDager = beregning.gjenståendeDager,
                    kilde = FagsystemDTO.DP_SAK,
                )
            }
        }
}

infix fun ClosedRange<LocalDate>.overlapper(b: ClosedRange<LocalDate>): Boolean = start <= b.endInclusive && b.start <= endInclusive
