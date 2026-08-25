package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DagpengestatusRequestDTO
import no.nav.dagpenger.datadeling.models.DagpengestatusResponseDTO
import java.time.LocalDate

class DagpengestatusService(
    private val dagpengestatusRepository: DagpengestatusRepository,
) {
    fun hentDagpengestatus(request: DagpengestatusRequestDTO): DagpengestatusResponseDTO =
        DagpengestatusResponseDTO(
            personIdent = request.personIdent,
            forsteDato =
                dagpengestatusRepository
                    .hent(request.personIdent)
                    .mapNotNull { it.tidligsteVirkningsDato() }
                    .minOrNull(),
        )

    private fun BehandlingResultat.tidligsteVirkningsDato(): LocalDate? = rettighetsperioder.minByOrNull { it.fraOgMed }?.fraOgMed
}
