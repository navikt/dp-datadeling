package no.nav.dagpenger.datadeling.api

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.behandling.arena.VedtakService
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.authentication
import no.nav.dagpenger.datadeling.api.plugins.configureMetrics
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.sporing.Log
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.søknad.SøknadService

fun Application.datadelingApi(
    logger: Log,
    config: AppConfig = Config.appConfig,
    perioderService: PerioderService,
    meldekortService: MeldekortService,
    søknaderService: SøknadService,
    vedtakService: VedtakService,
    ressursService: RessursService,
    behandlingRepository: BehandlingResultatRepositoryMedTolker,
) {
    authentication(config)
    configureMetrics()
    routing {
        afpPrivatRoutes(ressursService, perioderService, logger)
        dagpengerRoutes(perioderService, meldekortService, søknaderService, vedtakService, behandlingRepository, logger)
    }
}
