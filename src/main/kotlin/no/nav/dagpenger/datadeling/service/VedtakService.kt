package no.nav.dagpenger.datadeling.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class VedtakService(
    private val proxyClient: ProxyClient,
) {
    fun hentVedtak(request: DatadelingRequest): List<Vedtak> =
        runBlocking {
            val proxyResponse = async { proxyClient.hentVedtak(request) }

            // Sammenfletter og sorterer vedtak fra forskjellige kilder.
            // Vi henter foreløpig bare vedtak fra dp-proxy, men kommer til å måtte hente vedtak produsert i egen løsning senere.
            awaitAll(proxyResponse)
                .flatten()
                .sortedBy { it.fraOgMedDato }
        }
}
