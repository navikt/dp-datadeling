package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.datadeling.db.BehandlingRepository

private val logger = KotlinLogging.logger {}
private val sikkerLogg = KotlinLogging.logger("tjenestekall.VedtakFattetUtenforArenaMottak")

class VedtakFattetUtenforArenaMottak(
    rapidsConnection: RapidsConnection,
    private val behandlingRepository: BehandlingRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "vedtak_fattet_utenfor_arena") }
                validate { it.requireKey("behandlingId", "søknadId", "ident", "sakId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "Mottok hendelse om at vedtak har blitt fattet utenfor Arena" }
        sikkerLogg.info { "Mottok hendelse om at vedtak har blitt fattet utenfor Arena: ${packet.toJson()}" }

        try {
            val behandlingId = packet["behandlingId"].asText()
            val søknadId = packet["søknadId"].asText()
            val ident = packet["ident"].asText()
            val sakId = packet["sakId"].asText()

            behandlingRepository.lagreData(behandlingId, søknadId, ident, sakId)
        } catch (e: Exception) {
            logger.error(e) { "Feil ved behandling av hendelse om at vedtak har blitt fattet utenfor Arena" }
            sikkerLogg.error(e) { "Feil ved behandling av hendelse om at vedtak har blitt fattet utenfor Arena: ${packet.toJson()}" }
            throw e
        }
    }
}
