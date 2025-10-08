package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.datadeling.model.ArenaVedtaksmelding

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.ArenaVedtakMottak")

internal class ArenaVedtakMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("table", "SIAMO.VEDTAK") }
                validate {
                    it.requireKey(
                        "op_ts",
                        "after.VEDTAK_ID",
                        "after.SAK_ID",
                        "after.FRA_DATO",
                    )
                    it.requireAny("after.UTFALLKODE", listOf("JA", "NEI"))
                    // IVERK = Iverksatt, AVSLU = Avsluttet
                    it.requireAny("after.VEDTAKSTATUSKODE", listOf("IVERK", "AVSLU"))
                    // O = Krav om ny rettighet, E = Endring, G = Gjenopptak
                    it.requireAny("after.VEDTAKTYPEKODE", listOf("O", "E", "G"))
                    // DAGO	= Ordinære dagpenger, PERM = Dagpenger under permitteringer, FISK = Dagp. v/perm fra fiskeindustri
                    it.requireAny("after.RETTIGHETKODE", listOf("DAGO", "PERM", "FISK"))
                    it.interestedIn("after", "tokens")
                    it.interestedIn("after.TIL_DATO")
                    it.interestedIn("tokens.FODSELSNR")
                    it.interestedIn("FODSELSNR")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val arenaVedtaksmelding = ArenaVedtaksmelding(packet)

        withLoggingContext(
            "fagsakId" to arenaVedtaksmelding.fagsakId,
            "vedtakId" to arenaVedtaksmelding.vedtakId,
        ) {
            logg.info { "Mottok nytt vedtak" }
            sikkerlogg.info { "Mottok nytt vedtak for person ${arenaVedtaksmelding.ident}: ${packet.toJson()}" }

            // TODO: Konverter til en "ekstern"-hendelse og videresend til RnR
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.debug { problems }
        sikkerlogg.debug { problems.toExtendedReport() }
    }
}
