package no.nav.dagpenger.meldekort

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

class MeldekortService(
    private val meldekortregisterClient: MeldekortregisterClient,
) {
    suspend fun hentMeldekort(request: DatadelingRequestDTO) =
        // dp-meldekortregister returnerer alle meldekort (både fra meldekortregister og Arena) for personId
        meldekortregisterClient.hentMeldekort(request)
}
