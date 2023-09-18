package dp.datadeling.perioder

import dp.datadeling.utils.getProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.oauth2.CachedOauth2Client

class ProxyClient(private val client: HttpClient, private val tokenProvider: CachedOauth2Client) : PerioderClient {
    private val baseUrl = getProperty("DP_PROXY_URL")
    private val scope = getProperty("DP_PROXY_SCOPE")

    override suspend fun hentDagpengeperioder(request: DatadelingRequest) =
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