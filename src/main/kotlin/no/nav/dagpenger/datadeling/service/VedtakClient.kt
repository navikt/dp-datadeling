package no.nav.dagpenger.datadeling.service

import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

interface VedtakClient {
    suspend fun hentVedtak(request: DatadelingRequestDTO): List<Vedtak>
}
