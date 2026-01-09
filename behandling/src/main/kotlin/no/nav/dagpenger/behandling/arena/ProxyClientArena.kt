package no.nav.dagpenger.behandling.arena

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.behandling.PerioderClient
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.ktor.client.defaultHttpClient
import java.time.LocalDate

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

private data class ArenaPeriode(
    val fraOgMedDato: LocalDate,
    val ytelseType: ArenaYtelseType,
    val tilOgMedDato: LocalDate? = null,
)

private enum class ArenaYtelseType(
    val value: String,
) {
    DAGPENGER_ARBEIDSSOKER_ORDINAER("DAGPENGER_ARBEIDSSOKER_ORDINAER"),

    DAGPENGER_PERMITTERING_ORDINAER("DAGPENGER_PERMITTERING_ORDINAER"),

    DAGPENGER_PERMITTERING_FISKEINDUSTRI("DAGPENGER_PERMITTERING_FISKEINDUSTRI"),
}

private data class ArenaDatadelingResponse(
    val personIdent: String,
    val perioder: List<ArenaPeriode>,
)

class ProxyClientArena(
    private val dpProxyBaseUrl: String,
    private val tokenProvider: () -> String,
) : PerioderClient,
    VedtakClient {
    override suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): DatadelingResponseDTO {
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
                    }.body<ArenaDatadelingResponse>()
            }
        return result.fold(
            onSuccess = {
                it.let {
                    DatadelingResponseDTO(
                        personIdent = it.personIdent,
                        perioder =
                            it.perioder.map { periode ->
                                no.nav.dagpenger.datadeling.models.PeriodeDTO(
                                    fraOgMedDato = periode.fraOgMedDato,
                                    tilOgMedDato = periode.tilOgMedDato,
                                    ytelseType =
                                        when (periode.ytelseType) {
                                            ArenaYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER -> {
                                                no.nav.dagpenger.datadeling.models.YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
                                            }

                                            ArenaYtelseType.DAGPENGER_PERMITTERING_ORDINAER -> {
                                                no.nav.dagpenger.datadeling.models.YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER
                                            }

                                            ArenaYtelseType.DAGPENGER_PERMITTERING_FISKEINDUSTRI -> {
                                                no.nav.dagpenger.datadeling.models.YtelseTypeDTO.DAGPENGER_PERMITTERING_FISKEINDUSTRI
                                            }
                                        },
                                    kilde = no.nav.dagpenger.datadeling.models.PeriodeDTO.Kilde.ARENA,
                                )
                            },
                    )
                }
            },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente dagpengeperioder fra url: $urlString for request $request" }
                throw it
            },
        )
    }

    override suspend fun hentVedtak(request: DatadelingRequestDTO): List<Vedtak> {
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
