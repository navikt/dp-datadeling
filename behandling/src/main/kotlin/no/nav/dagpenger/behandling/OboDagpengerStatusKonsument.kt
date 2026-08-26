package no.nav.dagpenger.behandling

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.databind.ObjectMapper

/**
 * OBO er eneste konsument i dag, men ligger bak en egen adapter for å holde domeneflyten konsument-uavhengig.
 */
class OboDagpengerStatusKonsument(
    private val producer: KafkaProducer<String, String>,
    private val topic: String,
    private val objectMapper: ObjectMapper,
) : DagpengerStatusKonsument {
    override fun varsle(varsel: DagpengerHendelse) {
        if (varsel.meldingstype == DagpengerHendelse.Meldingstype.AVSLAG) {
            // Ignorer avslag, da det ikke er relevant for OBO ennå
        } else {
            producer.send(
                ProducerRecord(
                    topic,
                    varsel.ident,
                    objectMapper.writeValueAsString(
                        OboMelding(
                            personId = varsel.ident,
                            meldingstype = varsel.meldingstype,
                        ),
                    ),
                ),
            )
        }
    }

    private data class OboMelding(
        val personId: String,
        val meldingstype: DagpengerHendelse.Meldingstype,
    ) {
        val ytelsestype = "DAGPENGER"
        val kildesystem = "DPSAK"
    }
}
