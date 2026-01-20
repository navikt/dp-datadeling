package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDate
import java.util.UUID

private val logg = KotlinLogging.logger {}

class BehandlingResultatMottak(
    rapidsConnection: RapidsConnection,
    private val behandlingResultatRepository: BehandlingResultatRepository,
    private val behandlingResultatVarsler: BehandlingResultatVarsler,
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
                        "behandlingskjedeId",
                        "@opprettet",
                        "førteTil",
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

            val sakId: UUID = packet["behandlingskjedeId"].asText().let { UUID.fromString(it) }
            behandlingResultatRepository.lagre(
                ident = ident,
                behandlingId = behandlingId,
                basertPåId = basertPåId,
                sakId = sakId,
                json = json,
                opprettetTidspunkt = opprettetTidspunkt,
            )

            when (packet["førteTil"].asText()) {
                "Innvilgelse" -> behandlingResultatVarsler.varsleFørstegangsinnvilgelse(ident)

                // todo: hva gjør vi her, og hva skal vi kalle det?
                else -> behandlingResultatVarsler.varsleForlengelse(ident)
            }
        }
    }
}

class BehandlingResultatVarsler(
    private val producer: KafkaProducer<String, String>,
    private val topic: String,
    private val objectMapper: ObjectMapper,
) {
    fun varsleFørstegangsinnvilgelse(ident: String) {
        lagOgSendObomelding(ident, Obomelding.Meldingstype.OPPRETT)
    }

    fun varsleForlengelse(ident: String) {
        lagOgSendObomelding(ident, Obomelding.Meldingstype.OPPDATER)
    }

    private fun lagOgSendObomelding(
        ident: String,
        meldingstype: Obomelding.Meldingstype,
    ) {
        val obomelding = lagObomelding(ident, meldingstype)
        sendMelding(ident, obomelding)
    }

    private fun sendMelding(
        ident: String,
        melding: String,
    ) {
        val record = ProducerRecord(topic, ident, melding)
        producer.send(record)
    }

    private fun lagObomelding(
        ident: String,
        meldingstype: Obomelding.Meldingstype,
    ): String {
        val obomelding =
            Obomelding(
                personId = ident,
                meldingstype = meldingstype,
            )
        return objectMapper.writeValueAsString(obomelding)
    }

    private data class Obomelding(
        val personId: String,
        val meldingstype: Meldingstype,
    ) {
        val ytelsestype = "DAGPENGER"
        val kildesystem = "DPSAK"

        enum class Meldingstype {
            OPPRETT,
            OPPDATER,
        }
    }
}
