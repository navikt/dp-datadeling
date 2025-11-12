package no.nav.dagpenger.behandling

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
import java.time.LocalDate
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.BehandlingResultatMottak")

class BehandlingResultatMottak(
    rapidsConnection: RapidsConnection,
    private val sakIdHenter: SakIdHenter,
    private val behandlingResultatRepository: BehandlingResultatRepository,
    private val environment: String,
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
            if (behandlingId == UUID.fromString("019956ca-82af-7b6d-8235-453d0234b34e")) {
                return@withLoggingContext
            }
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

            val sakId: UUID =
                try {
                    runBlocking { sakIdHenter.hentSakId(behandlingId) }
                } catch (e: Exception) {
                    logg.error(e) { "Klarte ikke hente sakId for behandling=$behandlingId" }
                    if (environment == "prod-gcp") throw e else return@withLoggingContext
                }

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
