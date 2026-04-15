package no.nav.dagpenger.datadeling.api.plugins

import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import no.nav.dagpenger.datadeling.api.config.konsument

private const val KONSUMENT_KEY = "konsument"
private val KONSUMENT_ATTRIBUTE_KEY = AttributeKey.stringKey(KONSUMENT_KEY)

internal val ConsumerContextPlugin =
    createApplicationPlugin("ConsumerContextPlugin") {
        // Auth runs in the Plugins phase; by intercepting Call we have the principal available
        // and proceed() lets us wrap the entire handler execution
        application.intercept(ApplicationCallPipeline.Call) {
            val konsument = context.konsument()

            withLoggingContextAsync(KONSUMENT_KEY to konsument) {
                Span.current().setAttribute(KONSUMENT_ATTRIBUTE_KEY, konsument)
                proceed()
            }
        }
    }

fun Application.configureConsumerContext() {
    install(ConsumerContextPlugin)
}
