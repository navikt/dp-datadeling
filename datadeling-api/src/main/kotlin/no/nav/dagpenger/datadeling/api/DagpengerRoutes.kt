package no.nav.dagpenger.datadeling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.api.plugins.kreverTilgang
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.meldekort.MeldekortService

/**
 * Routes for datadeling av dagpenger-data til interne NAV-systemer.
 * Autentisering via Azure AD.
 */
fun Route.dagpengerRoutes(
    perioderService: PerioderService,
    meldekortService: MeldekortService,
) {
    authenticate("azure") {
        route("/dagpenger/datadeling/v1") {
            perioderRoute(perioderService)
            meldekortRoute(meldekortService)
        }
    }
}

private fun Route.perioderRoute(perioderService: PerioderService) {
    route("/perioder") {
        kreverTilgang(Tilgangsrolle.rettighetsperioder)
        post {
            val request = call.receive<DatadelingRequestDTO>()
            val response = perioderService.hentDagpengeperioder(request)

            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun Route.meldekortRoute(meldekortService: MeldekortService) {
    route("/meldekort") {
        kreverTilgang(Tilgangsrolle.meldekort)
        post {
            val request = call.receive<DatadelingRequestDTO>()

            try {
                val response = meldekortService.hentMeldekort(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                defaultLogger.error(e) { "Feil ved henting av meldekort" }
                throw e
            }
        }
    }
}
