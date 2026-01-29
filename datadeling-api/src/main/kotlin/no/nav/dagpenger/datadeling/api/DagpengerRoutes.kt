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
import no.nav.dagpenger.datadeling.api.config.clientId
import no.nav.dagpenger.datadeling.api.plugins.kreverTilgang
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.sporing.DagpengerMeldekortHentetHendelse
import no.nav.dagpenger.datadeling.sporing.DagpengerPerioderHentetHendelse
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.meldekort.MeldekortService

/**
 * Routes for datadeling av dagpenger-data til interne NAV-systemer.
 * Autentisering via Azure AD.
 */
fun Route.dagpengerRoutes(
    perioderService: PerioderService,
    meldekortService: MeldekortService,
    auditLogger: Log,
) {
    authenticate("azure") {
        route("/dagpenger/datadeling/v1") {
            perioderRoute(perioderService, auditLogger)
            meldekortRoute(meldekortService, auditLogger)
        }
    }
}

private fun Route.perioderRoute(
    perioderService: PerioderService,
    auditLogger: Log,
) {
    route("/perioder") {
        kreverTilgang(Tilgangsrolle.rettighetsperioder)
        post {
            val request = call.receive<DatadelingRequestDTO>()
            val response = perioderService.hentDagpengeperioder(request)

            auditLogger.log(
                DagpengerPerioderHentetHendelse(
                    saksbehandlerNavIdent = call.clientId(),
                    request = request,
                    response = response,
                ),
            )

            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun Route.meldekortRoute(
    meldekortService: MeldekortService,
    auditLogger: Log,
) {
    route("/meldekort") {
        kreverTilgang(Tilgangsrolle.meldekort)
        post {
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
        }
    }
}
