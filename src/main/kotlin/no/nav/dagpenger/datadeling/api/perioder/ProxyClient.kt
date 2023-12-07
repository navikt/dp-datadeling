package no.nav.dagpenger.datadeling.api.perioder

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.datadeling.config.DpProxyConfig
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.oauth2.CachedOauth2Client

class ProxyClient(
    proxyConfig: DpProxyConfig,
    private val client: HttpClient,
    private val tokenProvider: CachedOauth2Client,
) : PerioderClient {
    private val baseUrl = proxyConfig.url
    private val scope = proxyConfig.scope

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