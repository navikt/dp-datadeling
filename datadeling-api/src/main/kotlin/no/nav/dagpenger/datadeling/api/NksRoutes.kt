package no.nav.dagpenger.datadeling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.api.plugins.kreverTilgang
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.NksSakInkluderDTO
import java.util.UUID

/**
 * Routes for NKS: saker med status utledet fra behandlingene i dp-sak.
 * Autentisering via Azure AD.
 */
fun Route.nksRoutes(nksSakService: NksSakService) {
    authenticate("azure") {
        route("/dagpenger/datadeling/v1/nks/saker") {
            kreverTilgang(Tilgangsrolle.nks)
            post {
                val inkluder = call.valgteInkluder() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<DatadelingRequestDTO>()
                call.respond(HttpStatusCode.OK, nksSakService.hentSaker(request, inkluder))
            }
            get("/{sakId}") {
                val inkluder = call.valgteInkluder() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val sakId = call.sakId() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val sak = nksSakService.hentSak(sakId, inkluder)
                if (sak == null) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.OK, sak)
            }
        }
    }
}

private fun RoutingCall.sakId(): UUID? = runCatching { UUID.fromString(parameters["sakId"]) }.getOrNull()

// Returnerer null hvis include inneholder ukjente verdier
private fun RoutingCall.valgteInkluder(): Set<NksSakInkluderDTO>? {
    val verdier =
        request.queryParameters["include"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: return emptySet()
    val inkluder = verdier.map { NksSakInkluderDTO.fromValue(it) }
    return if (inkluder.any { it == null }) null else inkluder.filterNotNull().toSet()
}
