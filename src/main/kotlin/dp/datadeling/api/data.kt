package dp.datadeling.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import dp.datadeling.defaultLogger
import dp.datadeling.utils.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.logging.*
import no.nav.dagpenger.kontrakter.iverksett.AktivitetType
import no.nav.dagpenger.kontrakter.iverksett.DatoperiodeDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeDagpengerDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
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
                    defaultLogger.info { apiUrl }
                    defaultLogger.info { params.fnr }
                    val response = defaultHttpClient().get("$apiUrl/vedtakstatus/${params.fnr}")
                    defaultLogger.info { response.status.value }

                    when (response.status.value) {
                        in 200..299 -> {
                            // Les response fra dp-iverksett hvis status er OK
                            val vedtaksperiodeDagpengerDto: VedtaksperiodeDagpengerDto = response.body()
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
                    defaultLogger.error(e)
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