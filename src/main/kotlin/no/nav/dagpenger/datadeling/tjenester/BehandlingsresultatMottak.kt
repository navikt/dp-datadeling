package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.datadeling.db.BehandlingRepository
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.BehandlingsresultatMottak")

internal class BehandlingsresultatMottak(
    rapidsConnection: RapidsConnection,
    private val behandlingRepository: BehandlingRepository = BehandlingRepository(),
    private val vedtakRepository: VedtakRepository = VedtakRepository(),
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behandlingsresultat")
                }
                validate {
                    it.requireKey("ident", "behandlingId", "rettighetsperioder")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()

        withLoggingContext(
            "behandlingId" to behandlingId,
            "event_name" to "behandlingsresultat",
        ) {
            logg.info { "Mottok behandlingsresultat melding" }
            sikkerlogg.info { "Mottok behandlingsresultat melding: ${packet.toJson()}" }

            val ident = packet["ident"].asText()

            if (!ident.matches(Regex("[0-9]{11}"))) {
                logg.error { "Person-ident må ha 11 sifre" }
                return
            }

            // Her er vi avhengige av at vi får vedtak_fattet_utenfor_arena før behandlingsresultat
            val sakId = behandlingRepository.hentSakIdForBehandlingId(behandlingId)
            if (sakId == null) {
                logg.error { "Kan ikke finne søknadId for behandlingId $behandlingId" }
                throw Exception("Kan ikke finne søknadId for behandlingId $behandlingId")
            }

            // Slett alle vedtak for denne saken
            vedtakRepository.slettAlleVedtakForSak(sakId)

            packet["rettighetsperioder"].toList().forEachIndexed { index, rettighetsperiode ->
                val fraOgMed = rettighetsperiode["fraOgMed"].asLocalDate()
                val tilOgMed = rettighetsperiode["tilOgMed"]?.asLocalDate()
                val harRett = rettighetsperiode["harRett"].asBoolean()

                vedtakRepository.lagreVedtak(
                    "$behandlingId-$index",
                    sakId,
                    ident,
                    if (harRett) Vedtak.Utfall.INNVILGET else Vedtak.Utfall.AVSLÅTT,
                    StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER, // TODO: Finn ut riktig type
                    Vedtak.Kilde.DP,
                    fraOgMed,
                    tilOgMed,
                    null,
                    null,
                )
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.debug { problems }
    }
}
