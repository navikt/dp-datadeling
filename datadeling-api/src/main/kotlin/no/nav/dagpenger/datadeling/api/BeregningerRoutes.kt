package no.nav.dagpenger.datadeling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.dagpenger.behandling.BeregningerService
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.api.plugins.kreverTilgang
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO

/**
 * Routes for beregninger (utbetalingsdetaljer).
 * Kombinerer data fra både Arena og dp-sak.
 *
 * **POC: Dette endepunktet er foreløpig en Proof of Concept og kan endres uten forvarsel.**
 */
fun Route.beregningerRoutes(beregningerService: BeregningerService) {
    authenticate("azure") {
        route("/dagpenger/datadeling/v1/beregninger") {
            kreverTilgang(Tilgangsrolle.beregninger)
            post {
                val request = call.receive<DatadelingRequestDTO>()
                val response = beregningerService.hentBeregninger(request)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
