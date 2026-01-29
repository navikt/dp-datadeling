package no.nav.dagpenger.datadeling.api.plugins

import io.ktor.server.routing.Route
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle

/**
 * Installerer autorisering som krever at kallende applikasjon har minst én av de angitte tilgangene.
 */
fun Route.kreverTilgang(vararg tilganger: Tilgangsrolle) {
    install(AuthorizationPlugin) {
        this.tilganger = tilganger.map { it.name }.toSet()
    }
}
