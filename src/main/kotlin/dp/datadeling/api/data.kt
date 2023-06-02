package dp.datadeling.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.datadeling.utils.*
import no.nav.dagpenger.kontrakter.iverksett.AktivitetType
import no.nav.dagpenger.kontrakter.iverksett.DatoperiodeDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeDagpengerDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import com.papsign.ktor.openapigen.route.path.auth.get as authGet

fun NormalOpenAPIRoute.dataApi() {

    auth {
        route("/data/{fnr}") {
            authGet<DataParams, VedtaksperiodeDagpengerDto, TokenValidationContextPrincipal?>(
                info("Oppslag"),
                example = vedtaksperiodeDagpengerDtoExample
            ) { params ->
                try {
                    // Sjekk dp-iverksett
                    val apiUrl = getProperty("IVERKSETT_API_URL")!!
                    val client = HttpClient.newBuilder().build()
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create("$apiUrl/vedtakstatus/${params.fnr}"))
                        .build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                    when (response.statusCode()) {
                        in 200..299 -> {
                            // Les response fra dp-iverksett hvis status er OK
                            val body = response.body()
                            val vedtaksperiodeDagpengerDto = defaultObjectMapper.readValue(
                                body,
                                VedtaksperiodeDagpengerDto::class.java
                            )
                            // Svar
                            respondOk(vedtaksperiodeDagpengerDto)
                        }

                        404 -> {
                            // Sjekk Arena hvis status er NotFound
                            // Map Arena response to VedtaksperiodeDagpengerDto
                            // Svar
                            // TODO:
                            respondNotFound("Kunne ikke finne data")
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

data class DataParams(@PathParam("fnr") val fnr: String)

val vedtaksperiodeDagpengerDtoExample = VedtaksperiodeDagpengerDto(
    fraOgMedDato = LocalDate.now(),
    tilOgMedDato = LocalDate.now().plusDays(14),
    periode = DatoperiodeDto(
        fom = LocalDate.now(),
        tom = LocalDate.now().plusDays(10)
    ),
    aktivitet = AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
    periodeType = VedtaksperiodeType.HOVEDPERIODE
)