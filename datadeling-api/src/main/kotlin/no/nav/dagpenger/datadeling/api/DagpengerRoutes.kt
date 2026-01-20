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
import no.nav.dagpenger.behandling.BehandlingResultat
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.api.config.clientId
import no.nav.dagpenger.datadeling.api.plugins.AuthorizationPlugin
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.models.BeregnetDagDTO
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.sporing.DagpengerMeldekortHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.meldekort.MeldekortService

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun Route.dagpengerRoutes(
    perioderService: PerioderService,
    meldekortService: MeldekortService,
    behandlingRepository: BehandlingResultatRepositoryMedTolker,
    auditLogger: Log,
) {
    swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")

    authenticate("azure") {
        route("/dagpenger/datadeling/v1") {
            route("/perioder") {
                kreverTilgangerTil(Tilgangsrolle.rettighetsperioder)
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

            route("/meldekort") {
                kreverTilgangerTil(Tilgangsrolle.meldekort)
                post {
                    try {
                        val request = call.receive<DatadelingRequestDTO>()

                        val response = meldekortService.hentMeldekort(request)

                        auditLogger.log(
                            DagpengerMeldekortHentetHendelse(
                                saksbehandlerNavIdent = call.clientId(),
                                request = request,
                                response = response,
                            ),
                        )

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error { "Kunne ikke lese innholdet i forespørselen om meldekort. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om meldekort. Detaljer:" }
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Kunne ikke lese innholdet i forespørselen om meldekort",
                        )
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente meldekort. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente meldekort. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente meldekort")
                    }
                }
            }

            route("/beregninger") {
                kreverTilgangerTil(Tilgangsrolle.beregninger)
                post {
                    try {
                        val request = call.receive<DatadelingRequestDTO>()
                        val utbetalinger: List<BehandlingResultat> = behandlingRepository.hent(request.personIdent)

                        val response =
                            utbetalinger.flatMap { behandling ->
                                behandling.beregninger.map {
                                    BeregnetDagDTO(
                                        dato = it.dato,
                                        sats = it.sats,
                                        utbetaltBeløp = it.utbetaling,
                                    )
                                }
                            }

                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: BadRequestException) {
                        defaultLogger.error { "Kunne ikke lese innholdet i forespørselen om utbetaling. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke lese innholdet i forespørselen om utbetaling. Detaljer:" }
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke lese innholdet i forespørselen om utbetaling")
                    } catch (e: Exception) {
                        defaultLogger.error { "Kunne ikke hente utbetaling. Se sikkerlogg for detaljer" }
                        sikkerlogger.error(e) { "Kunne ikke hente utbetaling. Detaljer:" }
                        call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente utbetaling")
                    }
                }
            }
        }
    }
}

private fun Route.kreverTilgangerTil(vararg tilganger: Tilgangsrolle) {
    install(AuthorizationPlugin) {
        this.tilganger = tilganger.map { it.name }.toSet()
    }
}
