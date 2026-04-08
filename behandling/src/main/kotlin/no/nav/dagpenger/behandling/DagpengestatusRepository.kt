package no.nav.dagpenger.behandling

class DagpengestatusRepository(
    private val behandlingResultatRepository: BehandlingResultatRepository,
) {
    fun hent(ident: String): List<BehandlingResultat> = behandlingResultatRepository.hent(ident).map { BehandlingResultatV1Tolker.fra(it) }
}
