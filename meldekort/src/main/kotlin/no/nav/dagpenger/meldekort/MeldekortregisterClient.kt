package no.nav.dagpenger.meldekort

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.ktor.client.defaultHttpClient

class MeldekortregisterClient(
    private val dpMeldekortregisterUrl: String,
    private val tokenProvider: () -> String,
) {
    suspend fun hentMeldekort(request: DatadelingRequestDTO) =
        defaultHttpClient
            .post("$dpMeldekortregisterUrl/datadeling/meldekort") {
                bearerAuth(tokenProvider.invoke())
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(request)
            }.body<List<MeldekortDTO>>()
}
