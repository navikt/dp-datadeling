package no.nav.dagpenger.datadeling

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.client.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.api.internalApi
import no.nav.dagpenger.datadeling.perioder.PerioderService
import no.nav.dagpenger.datadeling.perioder.ProxyClient
import no.nav.dagpenger.datadeling.perioder.perioderApi
import no.nav.dagpenger.datadeling.ressurs.RessursConfig
import no.nav.dagpenger.datadeling.ressurs.RessursDao
import no.nav.dagpenger.datadeling.ressurs.RessursService
import no.nav.dagpenger.datadeling.teknisk.*
import no.nav.dagpenger.datadeling.teknisk.configureDataSource
import no.nav.dagpenger.datadeling.utils.LocalDateDeserializer
import no.nav.dagpenger.datadeling.utils.LocalDateSerializer
import no.nav.dagpenger.datadeling.utils.LocalDateTimeDeserializer
import no.nav.dagpenger.datadeling.utils.LocalDateTimeSerializer
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

val defaultLogger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.module(
    dataSource: DataSource = configureDataSource(environment.config),
    appConfig: AppConfig = AppConfig.fra(environment.config),
    ressursConfig: RessursConfig = RessursConfig.fra(environment.config),
    tokenProvider: CachedOauth2Client = cachedTokenProvider,
) {

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    install(OpenAPIGen) {
        serveOpenApiJson = true
        serveSwaggerUi = true
        swaggerUiPath = "internal/swagger-ui"
        info {
            title = "DP datadeling API"
        }
    }

    install(ContentNegotiation) {
        jackson {
            val javaTimeModule = JavaTimeModule()
            javaTimeModule.addSerializer(
                LocalDate::class.java,
                LocalDateSerializer()
            )
            javaTimeModule.addDeserializer(
                LocalDate::class.java,
                LocalDateDeserializer()
            )
            javaTimeModule.addSerializer(
                LocalDateTime::class.java,
                LocalDateTimeSerializer()
            )
            javaTimeModule.addDeserializer(
                LocalDateTime::class.java,
                LocalDateTimeDeserializer()
            )

            registerModule(javaTimeModule)
        }
    }

    val config = environment.config
    install(Authentication) {
        maskinporten(
            "afpPrivat",
            config.property("maskinporten.scope").getString(),
            config.property("maskinporten.jwks_uri").getString(),
            config.property("maskinporten.issuer").getString(),
        )
    }

    val client = HttpClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        installRetryClient(maksRetries = appConfig.maksRetries)
    }

    val ressursDao = RessursDao(dataSource)

    val perioderService = PerioderService(proxyClient = ProxyClient(appConfig, client, tokenProvider))

    val leaderElector = LeaderElector(client, appConfig)
    val ressursService = RessursService(ressursDao, leaderElector, ressursConfig)

    launch {
        ressursService.scheduleRessursCleanup()
    }

    routing {
//        internalApi(appMicrometerRegistry)
        perioderApi(appConfig, ressursService, perioderService)
    }
}
