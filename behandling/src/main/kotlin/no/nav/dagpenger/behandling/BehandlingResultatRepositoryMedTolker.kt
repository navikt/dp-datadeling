package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import java.time.LocalDate
import java.util.UUID

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

    fun hentSaker(ident: String): List<Sak> = behandlingResultatRepository.hentMedSak(ident).tilSaker()

    fun hentSak(sakId: UUID): Sak? = behandlingResultatRepository.hentMedSakId(sakId).tilSaker().singleOrNull()

    fun hentBehandling(behandlingId: UUID): SakBehandling? =
        behandlingResultatRepository
            .hentMedBehandlingId(
                behandlingId,
            )?.let { SakBehandling(it.opprettet, tolkerFactory.hentTolker(it.json)) }

    private fun List<LagretBehandlingResultat>.tilSaker(): List<Sak> =
        groupBy { it.sakId }.map { (sakId, lagrede) ->
            Sak(sakId, lagrede.map { SakBehandling(it.opprettet, tolkerFactory.hentTolker(it.json)) })
        }

    override suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): List<Periode> =
        hent(request.personIdent).flatMap { res ->
            // dp-sak
            res.rettighetsperioder.filter { it.harRett }.map { rettighetsperiode ->
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
