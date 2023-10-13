package no.nav.dagpenger.datadeling.perioder

import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

interface PerioderClient {
    suspend fun hentDagpengeperioder(request: DatadelingRequest): DatadelingResponse
}