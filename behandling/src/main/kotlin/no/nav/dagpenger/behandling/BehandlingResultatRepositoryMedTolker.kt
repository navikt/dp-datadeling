package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import java.time.LocalDate

/**
 * Eksempel på bruk av tolker factory i repository-wrapper.
 * Denne klassen kan brukes hvis du vil separere tolkning fra repository.
 */
class BehandlingResultatRepositoryMedTolker(
    private val behandlingResultatRepository: BehandlingResultatRepository,
    private val tolkerFactory: BehandlingResultatTolkerFactory = standardTolkerFactory,
) : PerioderClient {
    fun hent(ident: String): List<BehandlingResultat> {
        val resultat = behandlingResultatRepository.hent(ident)
        return resultat.map { jsonNode -> tolkerFactory.hentTolker(jsonNode) }
    }

    override suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): List<Periode> =
        hent(request.personIdent).flatMap { res ->
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

                Periode(
                    fraOgMed = rettighetsperiode.fraOgMed,
                    tilOgMed = rettighetsperiode.tilOgMed,
                    ytelseType =
                        when (rettighetstype) {
                            Rettighetstype.ORDINÆR -> YtelseType.Ordinær
                            Rettighetstype.PERMITTERING -> YtelseType.Permittering
                            Rettighetstype.LØNNSGARANTI -> throw IllegalArgumentException("Lønngaranti ikke støttet i datadeling")
                            Rettighetstype.FISK -> YtelseType.Fiskeindustri
                        },
                    kilde = Fagsystem.DP_SAK,
                )
            }
        }
}
