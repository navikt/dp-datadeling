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
import no.nav.dagpenger.behandling.arena.ArenaBeregning
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.api.plugins.AuthorizationPlugin
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.models.BeregnetDagDTO
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO

private val sikkerlogger = KotlinLogging.logger("tjenestekall")

fun Route.arenaRoutes(arenaClient: ProxyClientArena) {
    swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")

    authenticate("azure") {
        route("/arena/datadeling/v1") {
            route("/beregninger") {
                kreverTilgangerTil(Tilgangsrolle.beregninger)
                post {
                    try {
                        val request = call.receive<DatadelingRequestDTO>()
                        val utbetalinger: List<ArenaBeregning> = arenaClient.hentBeregninger(request)

                        val response =
                            utbetalinger.map {
                                BeregnetDagDTO(
                                    fraOgMed = it.meldekortFraDato,
                                    tilOgMed = it.meldekortTilDato,
                                    sats = it.innvilgetSats.toInt(),
                                    utbetaltBeløp = it.belop.toInt(),
                                    gjenståendeDager = 260,
                                    kilde = FagsystemDTO.ARENA,
                                )
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
