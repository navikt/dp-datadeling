package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DagpengestatusRequestDTO
import no.nav.dagpenger.datadeling.models.DagpengestatusResponseDTO
import java.time.LocalDate

class DagpengestatusRepository(
    private val behandlingResultatRepository: BehandlingResultatRepository,
) {
    fun hent(ident: String): List<BehandlingResultat> = behandlingResultatRepository.hent(ident).map { BehandlingResultatV1Tolker.fra(it) }
}

class DagpengestatusService(
    private val dagpengestatusRepository: DagpengestatusRepository,
) {
    fun hentDagpengestatus(request: DagpengestatusRequestDTO): DagpengestatusResponseDTO? {
        val førsteDato =
            dagpengestatusRepository
                .hent(request.personIdent)
                .mapNotNull { it.tidligsteInnvilgelseDato() }
                .minOrNull()
                ?: return null

        return DagpengestatusResponseDTO(
            personIdent = request.personIdent,
            forsteDagpengevedtakDato = førsteDato,
        )
    }

    private fun BehandlingResultat.tidligsteInnvilgelseDato(): LocalDate? =
        rettighetsperioder.filter { it.harRett }.minByOrNull { it.fraOgMed }?.fraOgMed

    private fun BehandlingResultat.tidligsteAvslagDato(): LocalDate? =
        rettighetsperioder.filter { !it.harRett }.minByOrNull { it.fraOgMed }?.fraOgMed

    // Fase 2: bytt til denne — innvilgelse trumfer, fall tilbake til avslag
    private fun BehandlingResultat.tidligsteDagpengedato(): LocalDate? = tidligsteInnvilgelseDato() ?: tidligsteAvslagDato()
}
