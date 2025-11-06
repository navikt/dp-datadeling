package no.nav.dagpenger.datadeling.api

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.service.MeldekortService
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService
import no.nav.dagpenger.datadeling.sporing.Log

fun Application.datadelingApi(
    logger: Log,
    config: AppConfig = Config.appConfig,
    perioderService: PerioderService,
    meldekortService: MeldekortService,
    søknaderService: SøknaderService,
    vedtakService: VedtakService,
    ressursService: RessursService,
) {
    konfigurerApi(config)
    routing {
        afpPrivatRoutes(ressursService, perioderService, logger)
        dagpengerRoutes(perioderService, meldekortService, søknaderService, vedtakService, logger)
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
