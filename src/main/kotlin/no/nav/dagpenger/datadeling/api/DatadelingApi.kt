package no.nav.dagpenger.datadeling.api

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
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

fun Application.datadelingApi(config: AppConfig = Config.appConfig) {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    konfigurerApi(appMicrometerRegistry, config)

    val proxyClient = ProxyClient(Config.dpProxyUrl, Config.dpProxyTokenProvider)
    val perioderService = PerioderService(proxyClient)

    val leaderElector = LeaderElector(config)
    val ressursDao = RessursDao()
    val ressursService = RessursService(ressursDao, leaderElector, config.ressurs)

    routing {
        livenessRoutes(appMicrometerRegistry)
        afpPrivatRoutes(ressursService, perioderService)
        dagpengerRoutes(perioderService)
    }

    launch {
        ressursService.scheduleRessursCleanup()
    }
}

fun HttpClientConfig<*>.installRetryClient(
    maksRetries: Int = 5,
    delayFunc: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    install(HttpRequestRetry) {
        delay { delayFunc(it) }
        retryOnServerErrors(maxRetries = maksRetries)
        exponentialDelay()
    }
}
