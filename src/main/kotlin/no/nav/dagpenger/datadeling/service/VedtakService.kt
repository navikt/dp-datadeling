package no.nav.dagpenger.datadeling.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class VedtakService(
    private val proxyClient: ProxyClient,
    private val vedtakRepository: VedtakRepository = VedtakRepository(),
) {
    fun hentVedtak(request: DatadelingRequest): List<Vedtak> =
        runBlocking {
            val proxyResponse = async { proxyClient.hentVedtak(request) }
            val vedtakRepositoryResponse =
                async {
                    vedtakRepository.hentVedtakFor(
                        request.personIdent,
                        request.fraOgMedDato,
                        request.tilOgMedDato,
                    )
                }

            // Sammenfletter og sorterer vedtak fra forskjellige kilder
            awaitAll(proxyResponse, vedtakRepositoryResponse)
                .flatten()
                .sortedBy { it.fraOgMedDato }
        }
}
