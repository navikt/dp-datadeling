package no.nav.dagpenger.datadeling.api.metrics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter

object ApiMetrics {
    private val meter: Meter = GlobalOpenTelemetry.getMeter("dp-datadeling-api")

    val konsumentKallCounter: LongCounter =
        meter
            .counterBuilder("datadeling.api.konsument.kall")
            .setDescription("Antall API-kall per konsument")
            .setUnit("1")
            .build()

    val konsumentKallDuration: DoubleHistogram =
        meter
            .histogramBuilder("datadeling.api.konsument.kall.duration")
            .setDescription("Varighet av API-kall per konsument")
            .setUnit("ms")
            .build()
}
