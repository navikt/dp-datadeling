package no.nav.dagpenger.datadeling.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry


fun Route.internalApi(prometheusMeterRegistry: PrometheusMeterRegistry) {
    route("/internal/liveness") {
        get {
            call.respond("Alive")
        }
    }

    route("/internal/readyness") {
        get {
            call.respond("Ready")
        }
    }

    route("/internal/prometheus") {
        get {
            call.respond(prometheusMeterRegistry.scrape())
        }
    }
}
