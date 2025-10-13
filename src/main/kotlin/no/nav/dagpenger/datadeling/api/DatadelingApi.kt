package no.nav.dagpenger.datadeling.api

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.launch
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.api.ressurs.LeaderElector
import no.nav.dagpenger.datadeling.api.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.ProxyClient
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService

fun Application.datadelingApi(config: AppConfig = Config.appConfig) {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    konfigurerApi(appMicrometerRegistry, config)

    val proxyClient = ProxyClient()
    val perioderService = PerioderService(proxyClient)
    val søknaderService = SøknaderService()
    val vedtakService = VedtakService(proxyClient)

    val leaderElector = LeaderElector(config)
    val ressursDao = RessursDao()
    val ressursService = RessursService(ressursDao, leaderElector, config.ressurs)

    routing {
        livenessRoutes(appMicrometerRegistry)
        afpPrivatRoutes(ressursService, perioderService)
        dagpengerRoutes(perioderService, søknaderService, vedtakService)
    }

    launch {
        ressursService.scheduleRessursCleanup()
    }
}
