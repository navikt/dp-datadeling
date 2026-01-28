package no.nav.dagpenger.behandling.arena

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.behandling.Fagsystem
import no.nav.dagpenger.behandling.Periode
import no.nav.dagpenger.behandling.PerioderClient
import no.nav.dagpenger.behandling.YtelseType
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
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
    override suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): List<Periode> {
        val urlString = ("$dpProxyBaseUrl/proxy/v1/arena/dagpengerperioder").replace("//p", "/p")

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
                    it.perioder.map { periode ->
                        Periode(
                            fraOgMed = periode.fraOgMedDato,
                            tilOgMed = periode.tilOgMedDato,
                            ytelseType =
                                when (periode.ytelseType) {
                                    ArenaYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER -> {
                                        YtelseType.Ordinær
                                    }

                                    ArenaYtelseType.DAGPENGER_PERMITTERING_ORDINAER -> {
                                        YtelseType.Permittering
                                    }

                                    ArenaYtelseType.DAGPENGER_PERMITTERING_FISKEINDUSTRI -> {
                                        YtelseType.Fiskeindustri
                                    }
                                },
                            kilde = Fagsystem.ARENA,
                        )
                    }
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

    suspend fun hentBeregninger(request: DatadelingRequestDTO): List<ArenaBeregning> {
        val urlString = ("$dpProxyBaseUrl/proxy/v1/arena/beregning").replace("//p", "/p")

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
                    }.body<List<ArenaBeregning>>()
            }
        return result.fold(
            onSuccess = { it },
            onFailure = {
                sikkerlogger.error(it) { "Kunne ikke hente vedtak fra url: $urlString for request $request" }
                throw it
            },
        )
    }

    private val token: String
        get() =
            try {
                tokenProvider.invoke()
            } catch (e: Exception) {
                sikkerlogger.error(e) { "Kunne ikke hente token" }
                throw e
            }
}
