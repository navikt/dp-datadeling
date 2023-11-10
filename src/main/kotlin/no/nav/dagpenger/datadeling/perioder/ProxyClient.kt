package no.nav.dagpenger.datadeling.perioder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.oauth2.CachedOauth2Client

class ProxyClient(
    appConfig: AppConfig,
    private val client: HttpClient,
    private val tokenProvider: CachedOauth2Client,
) : PerioderClient {
    private val baseUrl = appConfig.dpProxyUrl
    private val scope = appConfig.dpProxyScope

    override suspend fun hentDagpengeperioder(request: DatadelingRequest): DatadelingResponse =
        client.post("$baseUrl/proxy/v1/arena/dagpengerperioder") {
            val credentials = tokenProvider.clientCredentials(scope)
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${credentials.accessToken}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(request)
        }.body<DatadelingResponse>()
}