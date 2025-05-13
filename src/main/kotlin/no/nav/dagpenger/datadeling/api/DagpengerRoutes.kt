package no.nav.dagpenger.datadeling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.clientId
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun Route.dagpengerRoutes(
    perioderService: PerioderService,
    auditLogger: Log = Config.logger,
) {
    swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")

    authenticate("azure") {
        route("/dagpenger/datadeling/v1/perioder") {
            post {
                try {
                    val request = call.receive<DatadelingRequest>()

                    val response: DatadelingResponse = perioderService.hentDagpengeperioder(request)

                    auditLogger.log(
                        DagpengerPerioderHentetHendelse(
                            saksbehandlerNavIdent = call.clientId(),
                            request = request,
                            response = response,
                        ),
                    )

                    call.respond(HttpStatusCode.OK, response)
                } catch (e: BadRequestException) {
                    defaultLogger.error("Kunne ikke lese innholdet i forespørselen. Se sikkerlogg for detaljer")
                    sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen. Detaljer:" }
                    call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen")
                } catch (e: Exception) {
                    defaultLogger.error("Kunne ikke hente perioder. Se sikkerlogg for detaljer")
                    sikkerlogger.error(e) { "Kunne ikke hente perioder. Detaljer:" }
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente perioder")
                }
            }
        }
    }
}
