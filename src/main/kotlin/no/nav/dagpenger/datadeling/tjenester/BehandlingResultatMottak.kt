package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.db.BehandlingResultatRepository
import no.nav.dagpenger.datadeling.db.Rettighetsperiode
import no.nav.dagpenger.datadeling.service.SakIdHenter
import java.time.LocalDate
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.BehandlingResultatMottak")

class BehandlingResultatMottak(
    rapidsConnection: RapidsConnection,
    private val sakIdHenter: SakIdHenter,
    private val behandlingResultatRepository: BehandlingResultatRepository = BehandlingResultatRepository(),
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behandlingsresultat") }
                validate {
                    it.requireKey(
                        "behandlingId",
                        "rettighetsperioder",
                        "ident",
                        "@opprettet",
                    )
                    it.interestedIn("basertPå")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = UUID.fromString(packet["behandlingId"].asText())

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            logg.info { "Mottok nytt behandling resultat." }
            val json = packet.toJson()
            val ident = packet["ident"].asText()
            val opprettetTidspunkt = packet["@opprettet"].asLocalDateTime()
            val basertPåId = packet["basertPå"].textValue()?.let { UUID.fromString(it) }

            // dette er en midlertidig sjekk for å unngå lagring av rene avslag før de skal bo hos oss
            val rettighetsperioder: List<Rettighetsperiode> =
                packet["rettighetsperioder"].map {
                    object : Rettighetsperiode {
                        override val fraOgMed: LocalDate = it["fraOgMed"].asLocalDate()
                        override val tilOgMed: LocalDate? = it["tilOgMed"]?.asOptionalLocalDate()
                        override val harRett: Boolean = it["harRett"].asBoolean()
                    }
                }
            if (rettighetsperioder.size == 1 && !rettighetsperioder.first().harRett) {
                logg.info { "Behandlingsresultat har kun ett avslag, lagrer ikke." }
                return@withLoggingContext
            }

            val sakId: UUID = runBlocking { sakIdHenter.hentSakId(behandlingId) }

            behandlingResultatRepository.lagre(
                ident = ident,
                behandlingId = behandlingId,
                basertPåId = basertPåId,
                sakId = sakId,
                json = json,
                opprettetTidspunkt = opprettetTidspunkt,
            )
        }
    }
}
