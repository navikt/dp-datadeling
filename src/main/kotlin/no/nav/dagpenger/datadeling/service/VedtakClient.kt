package no.nav.dagpenger.datadeling.service

import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

interface VedtakClient {
    suspend fun hentVedtak(request: DatadelingRequest): List<Vedtak>
}
