package no.nav.dagpenger.datadeling.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.launch
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.api.config.konfigurerApi
import no.nav.dagpenger.datadeling.api.perioder.PerioderService
import no.nav.dagpenger.datadeling.api.perioder.ProxyClient
import no.nav.dagpenger.datadeling.api.perioder.perioderRoutes
import no.nav.dagpenger.datadeling.api.perioder.ressurs.LeaderElector
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.configureDataSource
import no.nav.dagpenger.datadeling.loadConfig
import javax.sql.DataSource

fun Application.datadelingApi(
    config: AppConfig = Config.config,
) {

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    konfigurerApi(appMicrometerRegistry, config)

    val httpClient = httpClient(config)

    val proxyClient = ProxyClient(Config.dpProxyUrl, Config.dpProxyTokenProvider)
    val perioderService = PerioderService(proxyClient)

    val leaderElector = LeaderElector(httpClient, config)
    val ressursDao = RessursDao()
    val ressursService = RessursService(ressursDao, leaderElector, config.ressurs)

    routing {
        livenessRoutes(appMicrometerRegistry)
        perioderRoutes(config, ressursService, perioderService)
    }

    launch {
        ressursService.scheduleRessursCleanup()
    }
}

private fun httpClient(appConfig: AppConfig) = HttpClient {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    installRetryClient(maksRetries = appConfig.httpClient.retries)
}

fun HttpClientConfig<*>.installRetryClient(
    maksRetries: Int = 5,
    delayFunc: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }, // Brukes for å mocke ut delay i enhetstester,
) {
    install(HttpRequestRetry) {
        delay { delayFunc(it) }
        retryOnServerErrors(maxRetries = maksRetries)
        exponentialDelay()
    }
}