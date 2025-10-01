package no.nav.dagpenger.datadeling.service

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Config.defaultHttpClient
import no.nav.dagpenger.datadeling.model.Meldekort
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class MeldekortregisterClient(
    private val dpMeldekortregisterUrl: String = Config.dpMeldekortregisterUrl,
    private val tokenProvider: () -> String = Config.dpMeldekortregisterTokenProvider,
) : MeldekortClient {
    override suspend fun hentMeldekort(request: DatadelingRequest): List<Meldekort> {
        defaultHttpClient.post("$dpMeldekortregisterUrl/datadeling/meldekort") {
            bearerAuth(tokenProvider.invoke())
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        return emptyList()
    }
}
