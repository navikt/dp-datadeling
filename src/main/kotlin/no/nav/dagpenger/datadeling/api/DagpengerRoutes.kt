package no.nav.dagpenger.datadeling.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Config.IDENT_REGEX
import no.nav.dagpenger.datadeling.api.config.clientId
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSisteSøknadHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerSøknaderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerVedtakHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log

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
                        val request = call.receive<DatadelingRequestDTO>()

                        val response: DatadelingResponseDTO = perioderService.hentDagpengeperioder(request)

                        auditLogger.log(
                            DagpengerPerioderHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = request,
                                response = response,
                            ),
                        )

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error { "Kunne ikke lese innholdet i forespørselen om perioder. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om perioder. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om perioder")
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente perioder. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente perioder. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente perioder")
                    }
                }
            }

            route("/soknader") {
                post {
                    try {
                        val request = call.receive<DatadelingRequestDTO>()

                        val response: List<Søknad> = søknaderService.hentSøknader(request)

                        auditLogger.log(
                            DagpengerSøknaderHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = request,
                                response = response,
                            ),
                        )

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error { "Kunne ikke lese innholdet i forespørselen om søknader. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om søknader. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om søknader")
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente søknader. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente søknader. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente søknader")
                    }
                }
            }

            route("/siste_soknad") {
                post {
                    try {
                        val ident = call.receive<String>()
                        if (IDENT_REGEX.matches(ident).not()) {
                            throw BadRequestException("Fødselsnummer/D-nummer må inneholde 11 siffer")
                        }

                        val søknad = søknaderService.hentSisteSøknad(ident)

                        auditLogger.log(
                            DagpengerSisteSøknadHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = ident,
                                response = søknad,
                            ),
                        )

                        if (søknad == null) {
                            call.respond(HttpStatusCode.NotFound)
                        } else {
                            call.respond(HttpStatusCode.OK, søknad)
                        }
                    } catch (e: BadRequestException) {
                        defaultLogger.error { "Kunne ikke lese ident fra forespørselen om siste søknad. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese ident fra forespørselen om siste søknad. Detaljer:" }
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Kunne ikke lese ident fra forespørselen om siste søknad",
                        )
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente siste søknad. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente siste søknad. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente siste søknad")
                    }
                }
            }

            route("/vedtak") {
                post {
                    try {
                        val request = call.receive<DatadelingRequestDTO>()

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
                        defaultLogger.error { "Kunne ikke lese innholdet i forespørselen om vedtak. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om vedtak. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om vedtak")
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente vedtak. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente vedtak. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente vedtak")
                    }
                }
            }
        }
    }
}
