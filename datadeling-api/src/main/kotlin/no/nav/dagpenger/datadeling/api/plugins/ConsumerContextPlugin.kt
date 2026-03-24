package no.nav.dagpenger.datadeling.api.plugins

import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import no.nav.dagpenger.datadeling.api.config.konsument

internal val ConsumerContextPlugin =
    createApplicationPlugin("ConsumerContextPlugin") {
        // Auth runs in the Plugins phase; by intercepting Call we have the principal available
        // and proceed() lets us wrap the entire handler execution
        application.intercept(ApplicationCallPipeline.Call) {
            val konsument = runCatching { context.konsument() }.getOrElse { "ukjent" }

            withLoggingContextAsync("konsument" to konsument) {
                Span.current().setAttribute(AttributeKey.stringKey("konsument"), konsument)
                proceed()
            }
        }
    }

fun Application.configureConsumerContext() {
    install(ConsumerContextPlugin)
}
