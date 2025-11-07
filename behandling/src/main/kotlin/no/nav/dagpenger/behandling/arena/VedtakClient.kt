package no.nav.dagpenger.behandling.arena

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

interface VedtakClient {
    suspend fun hentVedtak(request: DatadelingRequestDTO): List<Vedtak>
}
