package dp.datadeling.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.datadeling.defaultLogger
import dp.datadeling.utils.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.iverksett.*
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.LocalDateTime
import com.papsign.ktor.openapigen.route.path.auth.get as authGet


fun NormalOpenAPIRoute.dataApi() {

    auth {
        route("/data/v1.0/{fnr}") {
            authGet<DataParams, VedtaksstatusDto, TokenValidationContextPrincipal?>(
                info("Oppslag"),
                example = vedtaksstatusDtoExample
            ) { params ->
                try {
                    val client = HttpClient.newBuilder().build()

                    // Sjekk dp-iverksett
                    val dpIverksettCreds = cachedTokenProvider.clientCredentials(getProperty("DP_IVERKSETT_SCOPE")!!)
                    val dpIverksettUrl = getProperty("DP_IVERKSETT_URL")!!
                    val dpIverksettRequest = HttpRequest.newBuilder()
                        .uri(URI.create("$dpIverksettUrl/api/vedtakstatus/${params.fnr}"))
                        .header(HttpHeaders.Authorization, "Bearer ${dpIverksettCreds.accessToken}")
                        .build()
                    val dpIverksettResponse = client.send(dpIverksettRequest, HttpResponse.BodyHandlers.ofString())

                    when (dpIverksettResponse.statusCode()) {
                        in 200..299 -> {
                            // Les response fra dp-iverksett hvis status er OK
                            val body = dpIverksettResponse.body()
                            val vedtaksstatusDto = defaultObjectMapper.readValue(
                                body,
                                VedtaksstatusDto::class.java
                            )

                            // Svar
                            respondOk(vedtaksstatusDto)
                        }

                        404 -> {
                            // Sjekk Arena gjennom dp-proxy hvis status er NotFound
                            // Map Arena response to VedtaksperiodeDagpengerDto
                            // Svar
                            val dpProxyCreds = cachedTokenProvider.clientCredentials(getProperty("DP_PROXY_SCOPE")!!)
                            val dpProxyUrl = getProperty("DP_PROXY_URL")!!
                            val dpProxyRequest = HttpRequest.newBuilder()
                                .uri(URI.create("$dpProxyUrl/proxy/v1/arena/vedtaksstatus/${params.fnr}"))
                                .header(HttpHeaders.Authorization, "Bearer ${dpProxyCreds.accessToken}")
                                .build()
                            val dpProxyResponse = client.send(dpProxyRequest, HttpResponse.BodyHandlers.ofString())

                            when (dpProxyResponse.statusCode()) {
                                in 200..299 -> {
                                    // Les response fra dp-proxy hvis status er OK
                                    val body = dpProxyResponse.body()
                                    val list = defaultObjectMapper.readValue<List<DpProxyResponseDto>>(body)
                                    // TODO: Delete
                                    defaultLogger.info { body }

                                    // Finn siste vedtak som er innvilget (Godkjent eller Iverksatt)
                                    // TODO: Eller kun Iverksatt?
                                    val sisteVedtak =
                                        list.filter { it.vedtakstatuskode == "GODKJ" || it.vedtakstatuskode == "IVERK" }
                                            .maxByOrNull { it.vedtaksdato!! } // Alle vedtak som er GODJ og IVERK skal ha vedtaksdato != NULL

                                    if (sisteVedtak == null) {
                                        respondNotFound("Kunne ikke finne data")
                                    } else {
                                        // Map til VedtaksstatusDto
                                        val arenaVedtaksstatusDto = VedtaksstatusDto(
                                            vedtakstype = VedtakType.RAMMEVEDTAK, // TODO: Hvordan kan vi finne ut at det er et RAMMEVEDTAK? Fra vedtaktypekode?
                                            vedtakstidspunkt = sisteVedtak.vedtaksdato!!.atStartOfDay(), // Vi har ikke tidspunkt i vedtaksdato fra Arena. Derfor atStartOfDay
                                            resultat = Vedtaksresultat.INNVILGET, // sisteVedtak er innvilget (Godkjent eller Iverksatt)
                                            vedtaksperioder = listOf(
                                                VedtaksperiodeDto(
                                                    fraOgMedDato = sisteVedtak.fraDato!!,
                                                    tilOgMedDato = sisteVedtak.tilDato,
                                                    periodeType = VedtaksperiodeType.HOVEDPERIODE
                                                )
                                            )
                                        )

                                        // Svar
                                        respondOk(arenaVedtaksstatusDto)
                                    }
                                }

                                404 -> {
                                    respondNotFound("Kunne ikke finne data")
                                }

                                else -> {
                                    respondError("Kunne ikke få data fra dp-proxy")
                                }
                            }
                        }

                        else -> {
                            // Feil i dp-iverksett? Svar med status 500
                            respondError("Kunne ikke få data fra dp-iverksett")
                        }
                    }
                } catch (e: Exception) {
                    // Feil? Svar med status 500
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

data class DataParams(@PathParam("fnr") val fnr: String)

val vedtaksstatusDtoExample = VedtaksstatusDto(
    vedtakstype = VedtakType.RAMMEVEDTAK,
    vedtakstidspunkt = LocalDateTime.now(),
    resultat = Vedtaksresultat.INNVILGET,
    vedtaksperioder = listOf(
        VedtaksperiodeDto(
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now().plusDays(7),
            periodeType = VedtaksperiodeType.HOVEDPERIODE
        )
    ),
)

data class DpProxyResponseDto(
    val vedtaktypekode: String,
    val vedtakstatuskode: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
    val vedtaksdato: LocalDate?
)
