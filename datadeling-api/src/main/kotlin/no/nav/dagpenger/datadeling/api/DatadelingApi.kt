package no.nav.dagpenger.datadeling.api

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.BeregningerService
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.authentication
import no.nav.dagpenger.datadeling.api.plugins.configureMetrics
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.meldekort.MeldekortService

fun Application.datadelingApi(
    config: AppConfig = Config.appConfig,
    perioderService: PerioderService,
    meldekortService: MeldekortService,
    beregningerService: BeregningerService,
    ressursService: RessursService,
) {
    authentication(config)
    configureMetrics()

    routing {
        swaggerUI(path = "openapi", swaggerFile = "datadeling-api.yaml")
        afpPrivatRoutes(ressursService, perioderService)
        dagpengerRoutes(perioderService, meldekortService)
        beregningerRoutes(beregningerService)
    }
}
