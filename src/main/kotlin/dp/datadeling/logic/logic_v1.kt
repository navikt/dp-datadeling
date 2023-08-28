package dp.datadeling.logic

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import dp.datadeling.utils.getProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

fun process(request: DatadelingRequest): DatadelingResponse {
    val client = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }

    var response: DatadelingResponse

    // 2 request'er parallelt
    runBlocking {
        // Request til dp-iverksett
        val dpIverksettCreds = cachedTokenProvider.clientCredentials(getProperty("DP_IVERKSETT_SCOPE")!!)
        val dpIverksettUrl = getProperty("DP_IVERKSETT_URL")!!
        val dpIverksettResponse: Deferred<DatadelingResponse> = async {
            client.post("$dpIverksettUrl/api/vedtakstatus") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Bearer ${dpIverksettCreds.accessToken}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(request)
            }.body()
        }

        // Request til dp-proxy
        val dpProxyCreds = cachedTokenProvider.clientCredentials(getProperty("DP_PROXY_SCOPE")!!)
        val dpProxyUrl = getProperty("DP_PROXY_URL")!!
        val dpProxyResponse: Deferred<DatadelingResponse> = async {
            client.post("$dpProxyUrl/proxy/v1/arena/vedtaksstatus") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Bearer ${dpProxyCreds.accessToken}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(request)
            }.body()
        }

        // Venter på svar
        val dpIverksettResponseContent = dpIverksettResponse.await()
        val dpProxyResponseContent = dpProxyResponse.await()

        // Liste med alle perioder (både fra dp-iverksett og dp-proxy)
        val allePerioder = dpIverksettResponseContent.perioder + dpProxyResponseContent.perioder

        // Slå sammen perioder
        val sortertePerioder = allePerioder.sortedBy { it.fraOgMedDato }
        val slaattSammenPerioder = mutableListOf<Periode>()

        if (sortertePerioder.isNotEmpty()) {
            var periode = sortertePerioder[0]
            for (i in 1..<sortertePerioder.size) {
                periode = if (sortertePerioder[i].fraOgMedDato.minusDays(1) <= periode.tilOgMedDato) {
                    periode.copy(tilOgMedDato = sortertePerioder[i].tilOgMedDato)
                } else {
                    slaattSammenPerioder.add(periode)
                    sortertePerioder[i]
                }
            }
            slaattSammenPerioder.add(periode)
        }

        // Oppretter ett felles svar
        response = DatadelingResponse(
            personIdent = request.personIdent,
            perioder = slaattSammenPerioder.map {
                // Skjuler FOM- og TOM-datoer hvis mulig og viser ikke mer enn forespurt
                it.copy(
                    fraOgMedDato = maxOf(it.fraOgMedDato, request.fraOgMedDato),
                    tilOgMedDato = if (it.tilOgMedDato == null && request.tilOgMedDato != null) request.tilOgMedDato
                    else if (it.tilOgMedDato != null && request.tilOgMedDato == null) it.tilOgMedDato
                    else if (it.tilOgMedDato == null && request.tilOgMedDato == null) null
                    else minOf(it.tilOgMedDato!!, request.tilOgMedDato!!)
                )
            }.sortedBy { it.fraOgMedDato }
        )
    }

    return response
}

private val properties: Configuration by lazy {
    ConfigurationProperties.systemProperties() overriding EnvironmentVariables()
}

private val cachedTokenProvider by lazy {
    val azureAd = OAuth2Config.AzureAd(properties)
    CachedOauth2Client(
        tokenEndpointUrl = azureAd.tokenEndpointUrl,
        authType = azureAd.clientSecret(),
    )
}
