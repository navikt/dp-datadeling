package no.nav.dagpenger.datadeling.api.perioder

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.dagpenger.datadeling.config.AppConfig
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.api.perioder.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.util.*

fun Route.perioderApi(
    appConfig: AppConfig,
    ressursService: RessursService,
    perioderService: PerioderService,
) {


    authenticate("afpPrivat") {
        route("/maskinporten-test/") {
            get {
                defaultLogger.info("heipaadu")
                call.respond("Seherja")
            }
        }
    }

    authenticate("afpPrivat") {
        route("/dagpenger/v1/periode") {
            post {
                withContext(Dispatchers.IO) {
                    try {
                        val request = call.receive<DatadelingRequest>()
                        val ressurs = requireNotNull(ressursService.opprett(request))
                        val ressursUrl = "${appConfig.httpClient.host}/dagpenger/v1/periode/${ressurs.uuid}"

                        launch {
                            try {
                                val perioder = perioderService.hentDagpengeperioder(request)
                                ressursService.ferdigstill(ressurs.uuid, perioder)
                            } catch (e: Exception) {
                                ressursService.markerSomFeilet(ressurs.uuid)
                            }
                        }

                        call.respond(HttpStatusCode.Created, ressursUrl)
                    } catch (e: ContentTransformationException) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke lese innholdet i forespørselen")
                    } catch (e: Exception) {
                        defaultLogger.error { e }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette ressurs")
                    }
                }
            }

            get("/{uuid}") {
                try {
                    val ressursRef = UUID.fromString(call.parameters.get("uuid"))
                    val response = ressursService.hent(ressursRef)

                    if (response == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(HttpStatusCode.OK, response)
                    }

                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (e: Exception) {
                    defaultLogger.error { e }
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente ressurs")
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
