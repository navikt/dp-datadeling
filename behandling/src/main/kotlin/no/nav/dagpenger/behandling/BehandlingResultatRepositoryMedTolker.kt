package no.nav.dagpenger.behandling

/**
 * Eksempel på bruk av tolker factory i repository-wrapper.
 * Denne klassen kan brukes hvis du vil separere tolkning fra repository.
 */
class BehandlingResultatRepositoryMedTolker(
    private val behandlingResultatRepository: BehandlingResultatRepository,
    private val tolkerFactory: BehandlingResultatTolkerFactory = standardTolkerFactory,
) {
    fun hent(ident: String): List<BehandlingResultat> {
        val resultat = behandlingResultatRepository.hent(ident)
        return resultat.map { jsonNode ->
            tolkerFactory.hentTolker(jsonNode)
        }
    }
}
