package no.nav.dagpenger.datadeling.perioder

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.ressurs.Ressurs
import no.nav.dagpenger.datadeling.ressurs.RessursService
import no.nav.dagpenger.datadeling.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.teknisk.authAzureAd
import no.nav.dagpenger.datadeling.teknisk.authMaskinporten
import no.nav.dagpenger.datadeling.utils.respondCreated
import no.nav.dagpenger.datadeling.utils.respondError
import no.nav.dagpenger.datadeling.utils.respondOk
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDate
import java.util.*

fun NormalOpenAPIRoute.perioderApi(
    appConfig: AppConfig,
    ressursService: RessursService,
    perioderService: PerioderService,
) {
    authMaskinporten("afpprivat", appConfig.maskinportenUrl) {
        route("/maskinporten-test") {
            get<Unit, Ressurs, TokenValidationContextPrincipal?> {
                defaultLogger.info("heipaadu")
            }
        }
    }
    authAzureAd("azureAD") {
        route("/dagpenger/v1/periode") {
            post<Unit, String, DatadelingRequest, TokenValidationContextPrincipal?>(
                info("Opprett ressurs og motta endepunkt for å hente ressurs"),
                exampleRequest = requestExample,
                exampleResponse = "http://localhost:8080/api/dagpenger/v1/periode/{ressursId}"
            ) { _, request ->
                withContext(Dispatchers.IO) {
                    try {
                        val ressurs = requireNotNull(ressursService.opprett(request)) { "Kunne ikke opprette ressurs" }
                        val ressursUrl = "${appConfig.dpDatadelingUrl}/dagpenger/v1/periode/${ressurs.uuid}"

                        launch {
                            try {
                                val perioder = perioderService.hentDagpengeperioder(request)
                                ressursService.ferdigstill(ressurs.uuid, perioder)
                            } catch (e: Exception) {
                                ressursService.markerSomFeilet(ressurs.uuid)
                            }
                        }

                        respondCreated(ressursUrl)
                    } catch (e: Exception) {
                        defaultLogger.error { e }
                        respondError("Kunne ikke opprette ressurs")
                    }
                }
            }

            route("/{uuid}") {
                get<RessursId, Ressurs, TokenValidationContextPrincipal?>(
                    info("Hent ressurs"),
                    example = responseExample
                ) { params ->
                    try {
                        val response = requireNotNull(ressursService.hent(params.uuid)) {
                            "Kunne ikke hente ressurs"
                        }
                        respondOk(response)
                    } catch (e: Exception) {
                        defaultLogger.error { e }
                        respondError("Kunne ikke hente ressurs")
                    }
                }
            }
        }
    }
}

data class RessursId(@PathParam("Id for ressurs") val uuid: UUID)

private val requestExample = DatadelingRequest(
    personIdent = "01020312345",
    fraOgMedDato = LocalDate.now(),
    tilOgMedDato = LocalDate.now()
)

private val responseExample = Ressurs(
    uuid = UUID.randomUUID(),
    status = RessursStatus.FERDIG,
    response = DatadelingResponse(
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
)
