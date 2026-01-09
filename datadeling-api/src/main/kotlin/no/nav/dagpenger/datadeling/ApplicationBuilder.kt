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
import no.nav.dagpenger.behandling.BehandlingResultatMottak
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.behandling.arena.ProxyClientArena
import no.nav.dagpenger.behandling.arena.VedtakService
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.api.ressurs.LeaderElector
import no.nav.dagpenger.datadeling.api.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.db.BehandlingResultatRepositoryPostgresql
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.datadeling.db.SøknadRepositoryPostgresql
import no.nav.dagpenger.datadeling.sporing.KafkaLogger
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.meldekort.MeldekortregisterClient
import no.nav.dagpenger.søknad.SøknadMottak
import no.nav.dagpenger.søknad.SøknadRepository
import no.nav.dagpenger.søknad.SøknadService
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

    private val behandlingResultatRepositoryPostgresql = BehandlingResultatRepositoryPostgresql()
    private val config = Config.appConfig
    private val meldekortregisterClient = MeldekortregisterClient(Config.dpMeldekortregisterUrl, Config.dpMeldekortregisterTokenProvider)
    private val proxyClient = ProxyClientArena(Config.dpProxyUrl, Config.dpProxyTokenProvider)

    private val perioderService =
        PerioderService(proxyClient, BehandlingResultatRepositoryMedTolker(behandlingResultatRepositoryPostgresql))
    private val meldekortService = MeldekortService(meldekortregisterClient)
    private val søknadRepository: SøknadRepository = SøknadRepositoryPostgresql()
    private val søknaderService = SøknadService(søknadRepository)
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
                SøknadMottak(this, søknadRepository)
                BehandlingResultatMottak(
                    rapidsConnection = this,
                    behandlingResultatRepository = behandlingResultatRepositoryPostgresql,
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
