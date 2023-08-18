package dp.datadeling.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.datadeling.defaultLogger
import dp.datadeling.utils.auth
import dp.datadeling.utils.getProperty
import dp.datadeling.utils.respondError
import dp.datadeling.utils.respondOk
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
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDate
import com.papsign.ktor.openapigen.route.path.auth.post as authPost


fun NormalOpenAPIRoute.dataApi() {

    auth {
        route("/data/v1.0") {
            authPost<Unit, DatadelingResponse, DatadelingRequest, TokenValidationContextPrincipal?>(
                info("Oppslag"),
                exampleRequest = dataRequestExample,
                exampleResponse = dataResponseExample
            ) { _, request ->
                try {
                    val client = HttpClient {
                        install(ContentNegotiation) {
                            jackson {
                                registerModule(JavaTimeModule())
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            }
                        }
                    }

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

                        // Oppretter ett felles svar
                        val response = DatadelingResponse(
                            personIdent = request.personIdent,
                            perioder = dpIverksettResponseContent.perioder + dpProxyResponseContent.perioder
                        )

                        respondOk(response)
                    }
                } catch (e: Exception) {
                    // Feil? Svar med status 500
                    defaultLogger.error { e }
                    respondError("Kunne ikke få data", e)
                }
            }
        }
    }
}

private val properties: Configuration by lazy {
    systemProperties() overriding EnvironmentVariables()
}

private val cachedTokenProvider by lazy {
    val azureAd = OAuth2Config.AzureAd(properties)
    CachedOauth2Client(
        tokenEndpointUrl = azureAd.tokenEndpointUrl,
        authType = azureAd.clientSecret(),
    )
}

private val dataRequestExample = DatadelingRequest(
    personIdent = "01020312345",
    fraOgMedDato = LocalDate.now(),
    tilOgMedDato = LocalDate.now()
)

private val dataResponseExample = DatadelingResponse(
    personIdent = "01020312345",
    perioder = listOf(
        Periode(
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now(),
            ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
            gjenståendeDager = 0
        )
    )
)
