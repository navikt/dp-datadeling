package no.nav.dagpenger.meldekort

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

class MeldekortService(
    private val dpMeldekortClient: DpMeldekortClient,
    private val meldepliktAdapterClient: MeldepliktAdapterClient,
) {
    suspend fun hentMeldekort(request: DatadelingRequestDTO) =
        (dpMeldekortClient.hentMeldekort(request) + meldepliktAdapterClient.hentMeldekort(request))
            .sortedBy { it.periode.fraOgMed }
}
