package no.nav.dagpenger.meldekort

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

class MeldekortService(
    private val meldekortregisterClient: MeldekortregisterClient,
    private val meldepliktAdapterClient: MeldepliktAdapterClient,
) {
    suspend fun hentMeldekort(request: DatadelingRequestDTO) =
        (meldekortregisterClient.hentMeldekort(request) + meldepliktAdapterClient.hentMeldekort(request))
            .sortedBy { it.periode.fraOgMed }
}
