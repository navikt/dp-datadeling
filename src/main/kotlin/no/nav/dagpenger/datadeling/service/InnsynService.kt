package no.nav.dagpenger.datadeling.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Config.dpInnsynTokenProvider
import no.nav.dagpenger.datadeling.api.installRetryClient
import no.nav.dagpenger.datadeling.models.SoknadDTO
import no.nav.dagpenger.datadeling.models.VedtakDTO
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class InnsynService(
    private val dpInnsynBaseUrl: String = "http://dp-innsyn",
    private val tokenProvider: () -> String = dpInnsynTokenProvider,
) {
    suspend fun hentSoknader(request: DatadelingRequest): List<SoknadDTO> {
        val urlString = ("$dpInnsynBaseUrl/soknad")

        val token =
            try {
                tokenProvider.invoke()
            } catch (e: Exception) {
                sikkerlogger.error(e) { "Kunne ikke hente token" }
            }

        val result =
            runCatching {
                client
                    .post(urlString) {
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(HttpHeaders.ContentType, "application/json")
                        }
                        setBody(request)
                    }.body<List<SoknadDTO>>()
            }

        return result.fold(
            onSuccess = { it },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente søknader fra url: $urlString for request $request" }
                throw it
            },
        )
    }

    suspend fun hentVedtak(request: DatadelingRequest): List<VedtakDTO> {
        val urlString = ("$dpInnsynBaseUrl/vedtak")

        val token =
            try {
                tokenProvider.invoke()
            } catch (e: Exception) {
                sikkerlogger.error(e) { "Kunne ikke hente token" }
            }

        val result =
            runCatching {
                client
                    .post(urlString) {
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(HttpHeaders.ContentType, "application/json")
                        }
                        setBody(request)
                    }.body<List<VedtakDTO>>()
            }

        return result.fold(
            onSuccess = { it },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente vedtak fra url: $urlString for request $request" }
                throw it
            },
        )
    }

    private val client =
        HttpClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }
            installRetryClient(
                maksRetries = Config.dpProxyClientMaxRetries,
            )
            install(Logging) {
                level = LogLevel.INFO
            }
        }
}
