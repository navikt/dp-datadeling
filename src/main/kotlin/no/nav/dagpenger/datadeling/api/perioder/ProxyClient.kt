package no.nav.dagpenger.datadeling.api.perioder

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Config.dpProxyTokenProvider
import no.nav.dagpenger.datadeling.api.installRetryClient
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

class ProxyClient(
    private val dpProxyBaseUrl: String,
    private val tokenProvider: () -> String = dpProxyTokenProvider,
) : PerioderClient {

    override suspend fun hentDagpengeperioder(request: DatadelingRequest): DatadelingResponse {
        val urlString = ("$dpProxyBaseUrl/proxy/v1/arena/dagpengerperioder").replace("//", "/")

        val invoke = try {
            tokenProvider.invoke()
        } catch (e: Exception) {
            defaultLogger.error(e) { "Kunne ikke hente token " }
        }

        val result = runCatching {
            client.post(urlString) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Bearer $invoke")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(request)
            }.body<DatadelingResponse>()
        }
        return result.fold(
            onSuccess = { it },
            onFailure = {
                defaultLogger.error(it) { "Kunne ikke hente dagpengeperioder fra url: $urlString for request $request" }
                throw it
            }
        )
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        installRetryClient(
            maksRetries = Config.dpProxyClientMaxRetries
        )
        install(Logging) {
            level = LogLevel.INFO
        }
    }
}

