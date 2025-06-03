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
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSøknaderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerVedtakHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun Route.dagpengerRoutes(
    perioderService: PerioderService,
    søknaderService: SøknaderService,
    vedtakService: VedtakService,
    auditLogger: Log = Config.logger,
) {
    swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")

    authenticate("azure") {
        route("/dagpenger/datadeling/v1") {
            route("/perioder") {
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
                        defaultLogger.error("Kunne ikke lese innholdet i forespørselen om perioder. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om perioder. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om perioder")
                    } catch (e: Exception) {
                        defaultLogger.error("Kunne ikke hente perioder. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke hente perioder. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente perioder")
                    }
                }
            }

            route("/soknader") {
                post {
                    try {
                        val request = call.receive<DatadelingRequest>()

                        val response: List<Søknad> = søknaderService.hentSoknader(request)

                        auditLogger.log(
                            DagpengerSøknaderHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = request,
                                response = response,
                            ),
                        )

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error("Kunne ikke lese innholdet i forespørselen om søknader. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om søknader. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om søknader")
                    } catch (e: Exception) {
                        defaultLogger.error("Kunne ikke hente søknader. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke hente søknader. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente søknader")
                    }
                }
            }

            route("/vedtak") {
                post {
                    try {
                        val request = call.receive<DatadelingRequest>()

                        val response: List<Vedtak> = vedtakService.hentVedtak(request)

                        auditLogger.log(
                            DagpengerVedtakHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = request,
                                response = response,
                            ),
                        )

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error("Kunne ikke lese innholdet i forespørselen om vedtak. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om vedtak. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om vedtak")
                    } catch (e: Exception) {
                        defaultLogger.error("Kunne ikke hente vedtak. Se sikkerlogg for detaljer")
                        sikkerlogger.error(e) { "Kunne ikke hente vedtak. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente vedtak")
                    }
                }
            }
        }
    }
}
