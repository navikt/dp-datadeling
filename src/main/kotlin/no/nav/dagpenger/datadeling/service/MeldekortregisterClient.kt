package no.nav.dagpenger.datadeling.service

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Config.defaultHttpClient
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO

class MeldekortregisterClient(
    private val dpMeldekortregisterUrl: String = Config.dpMeldekortregisterUrl,
    private val tokenProvider: () -> String = Config.dpMeldekortregisterTokenProvider,
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
