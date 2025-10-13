package no.nav.dagpenger.datadeling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.defaultHttpClient
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class ProxyClient(
    private val dpProxyBaseUrl: String = Config.dpProxyUrl,
    private val tokenProvider: () -> String = Config.dpProxyTokenProvider,
) : PerioderClient,
    VedtakClient {
    override suspend fun hentDagpengeperioder(request: DatadelingRequest): DatadelingResponse {
        val urlString = ("$dpProxyBaseUrl/proxy/v1/arena/dagpengerperioder").replace("//p", "/p")

        val token =
            try {
                tokenProvider.invoke()
            } catch (e: Exception) {
                sikkerlogger.error(e) { "Kunne ikke hente token" }
            }

        val result =
            runCatching {
                defaultHttpClient
                    .post(urlString) {
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(HttpHeaders.ContentType, "application/json")
                        }
                        setBody(request)
                    }.body<DatadelingResponse>()
            }
        return result.fold(
            onSuccess = { it },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente dagpengeperioder fra url: $urlString for request $request" }
                throw it
            },
        )
    }

    override suspend fun hentVedtak(request: DatadelingRequest): List<Vedtak> {
        val urlString = ("$dpProxyBaseUrl/proxy/v1/arena/vedtaksliste").replace("//p", "/p")

        val token =
            try {
                tokenProvider.invoke()
            } catch (e: Exception) {
                sikkerlogger.error(e) { "Kunne ikke hente token" }
            }

        val result =
            runCatching {
                defaultHttpClient
                    .post(urlString) {
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(HttpHeaders.ContentType, "application/json")
                        }
                        setBody(request)
                    }.body<List<Vedtak>>()
            }
        return result.fold(
            onSuccess = { it },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente vedtak fra url: $urlString for request $request" }
                throw it
            },
        )
    }
}
