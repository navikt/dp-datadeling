package no.nav.dagpenger.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import java.time.LocalDate

class PerioderService(
    private val proxyClient: ProxyClientArena,
    private val repository: BehandlingResultatRepositoryMedTolker,
) {
    fun hentDagpengeperioder(request: DatadelingRequestDTO) =
        runBlocking {
            val proxyResponse = async { proxyClient.hentDagpengeperioder(request) } // arena
            val behandlingResultat =
                async {
                    val periodeDTO: List<PeriodeDTO> =
                        repository.hent(request.personIdent).flatMap { res ->
                            // dp-sak
                            res.rettighetsperioder.map { rettighetsperiode ->
                                val rettighetstype =
                                    res.rettighetstyper
                                        .firstOrNull { rettighetstyper ->
                                            (
                                                rettighetstyper.fraOgMed.isBefore(rettighetsperiode.fraOgMed) ||
                                                    rettighetstyper.fraOgMed.isEqual(rettighetsperiode.fraOgMed)
                                            ) &&
                                                (rettighetstyper.tilOgMed.isAfter(rettighetsperiode.tilOgMed ?: LocalDate.MAX))
                                        }?.type ?: Rettighetstype.ORDINÆR

                                PeriodeDTO(
                                    fraOgMedDato = rettighetsperiode.fraOgMed,
                                    tilOgMedDato = rettighetsperiode.tilOgMed,
                                    ytelseType =
                                        when (rettighetstype) {
                                            Rettighetstype.ORDINÆR -> {
                                                YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
                                            }

                                            Rettighetstype.PERMITTERING -> {
                                                YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER
                                            }

                                            Rettighetstype.LØNNSGARANTI -> {
                                                throw IllegalArgumentException("Lønngaranti ikke støttet i datadeling")
                                            }

                                            Rettighetstype.FISK -> {
                                                YtelseTypeDTO.DAGPENGER_PERMITTERING_FISKEINDUSTRI
                                            }
                                        },
                                    kilde = PeriodeDTO.Kilde.DP_SAK,
                                )
                            }
                        }
                    DatadelingResponseDTO(
                        personIdent = request.personIdent,
                        perioder = periodeDTO,
                    )
                }

            // Sammenfletter og sorterer dagpengeperioder fra forskjellige kilder.
            // Vi henter foreløpig bare perioder fra dp-proxy, men kommer til å måtte hente perioder fra vedtak produsert i
            // egen løsning senere.
            val perioder =
                awaitAll(proxyResponse, behandlingResultat)
                    .flatMap { it.perioder }
                    .sortedBy { it.fraOgMedDato }
                    .sammenslått()
                    .map { periode ->
                        periode.copy(
                            fraOgMedDato = maxOf(periode.fraOgMedDato, request.fraOgMedDato),
                            tilOgMedDato = listOfNotNull(periode.tilOgMedDato, request.tilOgMedDato).minOrNull(),
                        )
                    }.sortedBy { it.fraOgMedDato }

            DatadelingResponseDTO(
                personIdent = request.personIdent,
                perioder = perioder,
            )
        }
}

private fun List<PeriodeDTO>.sammenslått(): List<PeriodeDTO> =
    fold(emptyList()) { perioder, periode ->
        if (perioder.isEmpty()) {
            return@fold listOf(periode)
        }

        val forrige = perioder.last()

        if (periode.kanSlåsSammen(forrige)) {
            perioder.dropLast(1) + listOf(forrige.copy(tilOgMedDato = periode.tilOgMedDato))
        } else {
            perioder + listOf(periode)
        }
    }

private fun PeriodeDTO.kanSlåsSammen(forrige: PeriodeDTO): Boolean =
    this.ytelseType == forrige.ytelseType &&
        this.fraOgMedDato.minusDays(1) <= forrige.tilOgMedDato
