package no.nav.dagpenger.meldekort

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.delay
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO

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

    private val defaultHttpClient =
        HttpClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            install(HttpRequestRetry) {
                delay { delay(it) }
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
}
