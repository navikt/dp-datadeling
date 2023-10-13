package no.nav.dagpenger.datadeling.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheus.PrometheusMeterRegistry


fun NormalOpenAPIRoute.internalApi(prometheusMeterRegistry: PrometheusMeterRegistry) {
    route("/internal/liveness") {
        get<Unit, String> {
            respond("Alive")
        }
    }

    route("/internal/readyness") {
        get<Unit, String> {
            respond("Ready")
        }
    }

    route("/internal/prometheus") {
        get<Unit, String> {
            respond(prometheusMeterRegistry.scrape())
        }
    }
}
