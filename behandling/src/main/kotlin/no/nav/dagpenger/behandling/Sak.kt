package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class SakStatus { TIL_BEHANDLING, LOEPENDE, AVSLUTTET }

class SakBehandling(
    val opprettet: LocalDateTime,
    val resultat: BehandlingResultat,
)

/**
 * En sak er en sammenhengende behandlingskjede i dp-sak, identifisert av sakId (behandlingskjedeId).
 * Status utledes kun fra siste behandling i saken, ut fra rettighetsperiodene med harRett:
 * - Løpende: en periode er gyldig i dag (eller åpen). Den perioden er sakens gjeldende periode. Har perioden harRett=false
 *   og saken ellers har rett-perioder, er det en pause i en løpende sak.
 * - Avsluttet: alle periodene er slutt. Perioden som sluttet sist er gjeldende periode.
 * - TilBehandling: ellers, det vil si ingen perioder, eller bare fremtidige perioder uten at noen gjelder i dag.
 *   Saken har da ingen gjeldende periode, og hull før en fremtidig periode gir dermed aldri Løpende.
 */
class Sak(
    val sakId: UUID,
    behandlinger: List<SakBehandling>,
    private val idag: LocalDate = LocalDate.now(),
) {
    private val sisteBehandling = behandlinger.maxBy { it.opprettet }
    private val perioder = sisteBehandling.resultat.rettighetsperioder.filter { it.harRett }

    private val periodeIDag =
        sisteBehandling.resultat.rettighetsperioder
            .filter {
                it.fraOgMed <= idag && (it.tilOgMed == null || it.tilOgMed!! >= idag)
            }.maxByOrNull { it.fraOgMed }
    private val sisteAvsluttedePeriode =
        perioder.takeIf { alle -> alle.all { it.tilOgMed != null && it.tilOgMed!! < idag } }?.maxByOrNull { it.tilOgMed!! }

    val ident: String get() = sisteBehandling.resultat.ident
    val sistEndret: LocalDateTime get() = sisteBehandling.opprettet

    val rettighetsperioder get() = sisteBehandling.resultat.rettighetsperioder.sortedBy { it.fraOgMed }
    val harRett: Boolean get() = gjeldendePeriode?.harRett ?: false
    private val gjeldendePeriode get() = periodeIDag ?: sisteAvsluttedePeriode
    val fraOgMed: LocalDate? get() = gjeldendePeriode?.fraOgMed
    val tilOgMed: LocalDate? get() = gjeldendePeriode?.tilOgMed

    // Alle beregninger i saken. Tolkes først når de leses, og bør derfor bare brukes når de er etterspurt.
    val beregninger: List<BeregnetDag> by lazy { behandlinger.flatMap { it.resultat.beregninger }.sortedBy { it.dato } }

    // Kun siste behandling, og null hvis gamle data ikke lar seg tolke
    val gjenståendeDager: Int? by lazy {
        runCatching {
            sisteBehandling.resultat.beregninger
                .maxByOrNull { it.dato }
                ?.gjenståendeDager
        }.getOrNull()
    }

    fun status(): SakStatus =
        when {
            periodeIDag != null && (periodeIDag.harRett || perioder.isNotEmpty()) -> SakStatus.LOEPENDE
            sisteAvsluttedePeriode != null -> SakStatus.AVSLUTTET
            else -> SakStatus.TIL_BEHANDLING
        }
}
