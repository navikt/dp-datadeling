package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO

interface PerioderClient {
    suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): DatadelingResponseDTO
}
