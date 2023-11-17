package no.nav.dagpenger.datadeling.perioder

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode

class PerioderService(private val proxyClient: ProxyClient) {
    fun hentDagpengeperioder(request: DatadelingRequest) = runBlocking {
        val proxyResponse = async { proxyClient.hentDagpengeperioder(request) }

        // Sammenfletter og sorterer dagpengeperioder fra forskjellige kilder.
        // Vi henter foreløpig bare perioder fra dp-proxy, men kommer til å måtte hente perioder fra vedtak produsert i
        // egen løsning senere.
        val perioder = awaitAll(proxyResponse)
            .flatMap { it.perioder }
            .sortedBy { it.fraOgMedDato }
            .sammenslått()
            .map { periode ->
                periode.copy(
                    fraOgMedDato = maxOf(periode.fraOgMedDato, request.fraOgMedDato),
                    tilOgMedDato = listOfNotNull(periode.tilOgMedDato, request.tilOgMedDato).minOrNull()
                )
            }
            .sortedBy { it.fraOgMedDato }

        DatadelingResponse(
            personIdent = request.personIdent,
            perioder = perioder
        )
    }
}

private fun List<Periode>.sammenslått(): List<Periode> =
    fold(emptyList()) { perioder, periode ->
        if (perioder.isEmpty()) {
            return@fold listOf(periode)
        }

        val forrige = perioder.last()

        if (periode.kanSlåsSammen(forrige)) {
            perioder.dropLast(1) + listOf(forrige.copy(tilOgMedDato = periode.tilOgMedDato))
        } else {
            perioder + listOf(periode)
        }
    }

private fun Periode.kanSlåsSammen(forrige: Periode): Boolean =
    this.ytelseType == forrige.ytelseType
            && this.fraOgMedDato.minusDays(1) <= forrige.tilOgMedDato