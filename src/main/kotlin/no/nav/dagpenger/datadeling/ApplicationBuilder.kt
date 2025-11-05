package no.nav.dagpenger.datadeling

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.tracer.initializer.SpanContextSupplier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.api.ressurs.LeaderElector
import no.nav.dagpenger.datadeling.api.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.db.BehandlingResultatRepository
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.datadeling.service.MeldekortService
import no.nav.dagpenger.datadeling.service.MeldekortregisterClient
import no.nav.dagpenger.datadeling.service.PerioderService
import no.nav.dagpenger.datadeling.service.ProxyClient
import no.nav.dagpenger.datadeling.service.SakIdHenter
import no.nav.dagpenger.datadeling.service.SøknaderService
import no.nav.dagpenger.datadeling.service.VedtakService
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

    private val behandlingResultatRepository = BehandlingResultatRepository()
    private val config = Config.appConfig
    private val meldekortregisterClient = MeldekortregisterClient()
    private val proxyClient = ProxyClient()

    private val perioderService = PerioderService(proxyClient, behandlingResultatRepository)
    private val meldekortService = MeldekortService(meldekortregisterClient)
    private val søknaderService = SøknaderService()
    private val vedtakService = VedtakService(proxyClient)

    private val leaderElector = LeaderElector(config)
    private val ressursDao = RessursDao()
    private val ressursService = RessursService(ressursDao, leaderElector, config.ressurs)

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
                            datadelingApi(
                                logger = KafkaLogger(rapid),
                                config = config,
                                perioderService = perioderService,
                                meldekortService = meldekortService,
                                søknaderService = søknaderService,
                                vedtakService = vedtakService,
                                ressursService = ressursService,
                            )
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
                    behandlingResultatRepository = behandlingResultatRepository,
                )
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
        startRessursRydder()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startRessursRydder() {
        GlobalScope.launch {
            ressursService.scheduleRessursCleanup()
        }
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        log.info { "Starter dp-datadeling" }
        runMigration().takeIf { it > 0 }?.let {
            log.info { "Migrerte database (dp-datadeling) antall migreringer: $it" }
        }
    }
}
