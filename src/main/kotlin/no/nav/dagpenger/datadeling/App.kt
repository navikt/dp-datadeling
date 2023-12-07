package no.nav.dagpenger.datadeling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.api.internalApi
import no.nav.dagpenger.datadeling.api.perioder.PerioderService
import no.nav.dagpenger.datadeling.api.perioder.ProxyClient
import no.nav.dagpenger.datadeling.api.perioder.perioderApi
import no.nav.dagpenger.datadeling.api.perioder.ressurs.LeaderElector
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.config.AppConfig
import no.nav.dagpenger.datadeling.config.configureDataSource
import no.nav.dagpenger.datadeling.config.loadConfig
import no.nav.dagpenger.datadeling.teknisk.cachedTokenProvider
import no.nav.dagpenger.datadeling.teknisk.installRetryClient
import no.nav.dagpenger.datadeling.teknisk.maskinporten
import no.nav.dagpenger.datadeling.utils.javaTimeModule
import no.nav.dagpenger.oauth2.CachedOauth2Client
import javax.sql.DataSource

val defaultLogger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(
    appConfig: AppConfig = loadConfig(),
    dataSource: DataSource = configureDataSource(appConfig.db),
    tokenProvider: CachedOauth2Client = cachedTokenProvider,
) {

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val httpClient = httpClient(appConfig)

    val proxyClient = ProxyClient(appConfig.dpProxy, httpClient, tokenProvider)
    val perioderService = PerioderService(proxyClient = proxyClient)

    val leaderElector = LeaderElector(httpClient, appConfig)
    val ressursDao = RessursDao(dataSource)
    val ressursService = RessursService(ressursDao, leaderElector, appConfig.ressurs)

    routing {
        internalApi(appMicrometerRegistry)
        perioderApi(appConfig, ressursService, perioderService)
    }

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(javaTimeModule)
        }
    }

    install(Authentication) {
        maskinporten(name = "afpPrivat", maskinportenConfig = appConfig.maskinporten)
    }

    launch {
        ressursService.scheduleRessursCleanup()
    }
}

private fun httpClient(appConfig: AppConfig) = HttpClient {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    installRetryClient(maksRetries = appConfig.httpClient.retries)
}
