package no.nav.dagpenger.datadeling.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO

class PerioderService(
    private val proxyClient: ProxyClient,
) {
    fun hentDagpengeperioder(request: DatadelingRequestDTO) =
        runBlocking {
            val proxyResponse = async { proxyClient.hentDagpengeperioder(request) }

            // Sammenfletter og sorterer dagpengeperioder fra forskjellige kilder.
            // Vi henter foreløpig bare perioder fra dp-proxy, men kommer til å måtte hente perioder fra vedtak produsert i
            // egen løsning senere.
            val perioder =
                awaitAll(proxyResponse)
                    .flatMap { it.perioder }
                    .sortedBy { it.fraOgMedDato }
                    .sammenslått()
                    .map { periode ->
                        periode.copy(
                            fraOgMedDato = maxOf(periode.fraOgMedDato, request.fraOgMedDato),
                            tilOgMedDato = listOfNotNull(periode.tilOgMedDato, request.tilOgMedDato).minOrNull(),
                        )
                    }.sortedBy { it.fraOgMedDato }

            DatadelingResponseDTO(
                personIdent = request.personIdent,
                perioder = perioder,
            )
        }
}

private fun List<PeriodeDTO>.sammenslått(): List<PeriodeDTO> =
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

private fun PeriodeDTO.kanSlåsSammen(forrige: PeriodeDTO): Boolean =
    this.ytelseType == forrige.ytelseType &&
        this.fraOgMedDato.minusDays(1) <= forrige.tilOgMedDato
