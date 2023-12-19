package no.nav.dagpenger.datadeling.api.perioder

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.orgNummer
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.sporing.AuditLogger
import no.nav.dagpenger.datadeling.sporing.DagpengerPeriodeSpørringHendelse
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.util.UUID

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun Route.perioderRoutes(
    ressursService: RessursService,
    perioderService: PerioderService,
    auditLogger: AuditLogger = Config.auditLogger,
) {
    swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")

    authenticate("afpPrivat") {
        route("/dagpenger/v1/periode") {
            post {
                withContext(Dispatchers.IO) {
                    try {
                        val request = call.receive<DatadelingRequest>()
                        val ressurs = requireNotNull(ressursService.opprett(request))
                        val ressursUrl = "${Config.dpDatadelingUrl}/dagpenger/v1/periode/${ressurs.uuid}"
                        auditLogger.log(
                            DagpengerPeriodeSpørringHendelse(
                                ident = request.personIdent,
                                saksbehandlerNavIdent = call.orgNummer(),
                            ),
                        )

                        launch {
                            try {
                                val perioder: DatadelingResponse = perioderService.hentDagpengeperioder(request)
                                ressursService.ferdigstill(ressurs.uuid, perioder)
                            } catch (e: Exception) {
                                ressursService.markerSomFeilet(ressurs.uuid)
                            }
                        }

                        call.respond(HttpStatusCode.Created, ressursUrl)
                    } catch (e: ContentTransformationException) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke lese innholdet i forespørselen")
                    } catch (e: Exception) {
                        defaultLogger.error("Kunne ikke opprette ressurs. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke opprette ressurs. Detaljer:" }
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
                    defaultLogger.error("Kunne ikke hente ressurs. Se sikkerlogg for detaljer")
                    sikkerlogger.error(e) { "Kunne ikke hente ressurs. Detaljer:" }
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente ressurs")
                }
            }
        }
    }
}
