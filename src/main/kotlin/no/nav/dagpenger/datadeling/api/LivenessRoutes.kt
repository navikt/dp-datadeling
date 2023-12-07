package no.nav.dagpenger.datadeling.api

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.livenessRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
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
