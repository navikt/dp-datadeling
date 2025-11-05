package no.nav.dagpenger.datadeling

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.tracer.initializer.SpanContextSupplier
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.datadeling.service.SakIdHenter
import no.nav.dagpenger.datadeling.sporing.KafkaLogger
import no.nav.dagpenger.datadeling.tjenester.BehandlingResultatMottak
import no.nav.dagpenger.datadeling.tjenester.SøknadMottak
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

private val log = KotlinLogging.logger {}

internal class ApplicationBuilder(
    configuration: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val meterRegistry =
        PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            PrometheusRegistry.defaultRegistry,
            Clock.SYSTEM,
            SpanContextSupplier.getSpanContext(),
        )

    private val rapidsConnection =
        RapidApplication
            .create(
                configuration,
                builder = {
                    withKtor { preStopHook, rapid ->
                        naisApp(
                            meterRegistry =
                            meterRegistry,
                            objectMapper = objectMapper,
                            applicationLogger = LoggerFactory.getLogger("ApplicationLogger"),
                            callLogger = LoggerFactory.getLogger("CallLogger"),
                            aliveCheck = rapid::isReady,
                            readyCheck = rapid::isReady,
                            preStopHook = preStopHook::handlePreStopRequest,
                        ) {
                            datadelingApi(KafkaLogger(rapid))
                        }
                    }
                },
            ).apply {
                SøknadMottak(this)
                BehandlingResultatMottak(
                    rapidsConnection = this,
                    sakIdHenter =
                        SakIdHenter(
                            baseUrl = Config.sakApiBaseUrl,
                            tokenProvider = Config.sakApiToken,
                        ),
                )
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        log.info { "Starter dp-datadeling" }
        runMigration().takeIf { it > 0 }?.let {
            log.info { "Migrerte database (dp-datadeling) antall migreringer: $it" }
        }
    }
}
