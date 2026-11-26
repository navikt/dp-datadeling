package no.nav.dagpenger.datadeling.api.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.CancellationException

private val logger = KotlinLogging.logger { }

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            logger.warn { "Ugyldig forespørsel til ${call.request.httpMethod.value} ${call.request.path()}: ${cause.javaClass.simpleName}" }
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<NotFoundException> { call, _ -> call.respond(HttpStatusCode.NotFound) }
        exception<UnsupportedMediaTypeException> { call, _ -> call.respond(HttpStatusCode.UnsupportedMediaType) }

        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause

            val span = Span.current()
            span.recordException(cause)
            span.setStatus(StatusCode.ERROR, cause.javaClass.simpleName)

            logger.error(cause) {
                "Uventet feil i ${call.request.httpMethod.value} ${call.request.path()}. "
            }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
