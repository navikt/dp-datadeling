package no.nav.dagpenger.datadeling.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.path
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.dagpenger.datadeling.api.config.applikasjon
import no.nav.dagpenger.datadeling.api.metrics.ApiMetrics

internal val OtelTraceIdPlugin =
    createApplicationPlugin("OtelTraceIdPlugin") {
        onCallRespond { call, _ ->
            val traceId = runCatching { Span.current().spanContext.traceId }.getOrNull()
            traceId?.let { call.response.headers.append("X-Trace-Id", it) }
        }
    }

private val startTimeKey = io.ktor.util.AttributeKey<Long>("MetricsStartTime")

internal val KonsumentMetricsPlugin =
    createApplicationPlugin(
        name = "KonsumentMetricsPlugin",
    ) {
        onCall { call ->
            call.attributes.put(startTimeKey, System.currentTimeMillis())
            val konsument = konsument(call)
            val path = call.request.path()
            val attributes =
                Attributes.of(
                    AttributeKey.stringKey("konsument"),
                    konsument,
                    AttributeKey.stringKey("path"),
                    path,
                    AttributeKey.stringKey("method"),
                    call.request.local.method.value,
                )

            ApiMetrics.konsumentKallCounter.add(1, attributes)
        }

        onCallRespond { call, _ ->
            val startTime = call.attributes.getOrNull(startTimeKey) ?: return@onCallRespond
            val duration = System.currentTimeMillis() - startTime
            val konsument = konsument(call)
            val path = call.request.path()
            val attributes =
                Attributes.of(
                    AttributeKey.stringKey("konsument"),
                    konsument,
                    AttributeKey.stringKey("path"),
                    path,
                    AttributeKey.stringKey("method"),
                    call.request.local.method.value,
                )

            ApiMetrics.konsumentKallDuration.record(duration.toDouble(), attributes)
        }
    }

private fun konsument(call: PipelineCall): String {
    val konsument =
        try {
            call.applikasjon()
        } catch (e: Exception) {
            "ukjent"
        }
    return konsument
}

fun Application.configureMetrics() {
    install(KonsumentMetricsPlugin)
    install(OtelTraceIdPlugin)
}
