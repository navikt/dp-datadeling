package no.nav.dagpenger.datadeling.api.perioder

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.datadeling.Config.dpProxyTokenProvider
import no.nav.dagpenger.datadeling.api.installRetryClient
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

class ProxyClient(
    private val dpProxyBaseUrl: String,
    private val tokenProvider: () -> String = dpProxyTokenProvider,
) : PerioderClient {

    override suspend fun hentDagpengeperioder(request: DatadelingRequest): DatadelingResponse =
        client.post("$dpProxyBaseUrl/proxy/v1/arena/dagpengerperioder") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(request)
        }.body<DatadelingResponse>()

    private val client = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        installRetryClient()
    }
}

