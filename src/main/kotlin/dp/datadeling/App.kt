/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package dp.datadeling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import dp.datadeling.api.internalApi
import dp.datadeling.utils.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import no.nav.security.token.support.v2.tokenValidationSupport
import java.time.LocalDate
import java.time.LocalDateTime

val defaultLogger = KotlinLogging.logger {}
val defaultAuthProvider = JwtProvider()

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {

    // Install Micrometer/Prometheus
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    // Install CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    // Install OpenAPI plugin (Swagger UI)
    install(OpenAPIGen) {
        // Serve OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // Serve Swagger UI on %swaggerUiPath%/index.html
        serveSwaggerUi = true
        swaggerUiPath = "internal/swagger-ui"
        info {
            title = "DP datadeling API"
        }
        // Use JWT authentication (Authorize button appears in Swagger UI)
        addModules(defaultAuthProvider)
    }

    // Install JSON support
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

    // Install Authentication
    val conf = this.environment.config
    install(Authentication) {
        // Skip validation only if runs locally
        if (isLocal()) {
            basic {
                skipWhen { true }
            }
        } else {
            tokenValidationSupport(config = conf)
        }
    }

    apiRouting {
        internalApi(appMicrometerRegistry)
    }
}
