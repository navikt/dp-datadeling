package no.nav.dagpenger.datadeling.service

import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class MeldekortService(
    private val meldekortregisterClient: MeldekortregisterClient,
    private val proxyClient: ProxyClient,
) {
    fun hentMeldekort(request: DatadelingRequest) =
        runBlocking {
            // dp-meldekortregister returnerer alle meldekort (både fra meldekortregister og Arena) for personId
            meldekortregisterClient.hentMeldekort(request)
        }
}
