package no.nav.dagpenger.datadeling.service

import no.nav.dagpenger.datadeling.model.Meldekort
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

interface MeldekortClient {
    suspend fun hentMeldekort(request: DatadelingRequest): List<Meldekort>
}
