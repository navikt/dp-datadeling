package no.nav.dagpenger.behandling.arena

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

class VedtakService(
    private val proxyClient: ProxyClientArena,
) {
    fun hentVedtak(request: DatadelingRequestDTO): List<Vedtak> =
        runBlocking {
            val proxyResponse = async { proxyClient.hentVedtak(request) }

            // Sammenfletter og sorterer vedtak fra forskjellige kilder.
            // Vi henter foreløpig bare vedtak fra dp-proxy, men kommer til å måtte hente vedtak produsert i egen løsning senere.
            awaitAll(proxyResponse)
                .flatten()
                .sortedBy { it.fraOgMedDato }
        }
}
