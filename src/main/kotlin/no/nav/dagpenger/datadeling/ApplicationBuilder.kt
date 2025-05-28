package no.nav.dagpenger.datadeling

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.datadeling.tjenester.SøknadMottak
import no.nav.dagpenger.datadeling.tjenester.VedtakMottak
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    configuration: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val rapidsConnection =
        RapidApplication
            .create(
                configuration,
                builder = {
                    withKtorModule {
                        datadelingApi()
                    }
                },
            ).apply {
                SøknadMottak(this)
                VedtakMottak(this)
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        runMigration()
    }
}
