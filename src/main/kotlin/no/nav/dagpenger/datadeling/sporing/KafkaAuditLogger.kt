package no.nav.dagpenger.datadeling.sporing

import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

internal class KafkaAuditLogger(private val rapidsConnection: RapidsConnection) : AuditLogger {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()

    override fun log(hendelse: AuditHendelse) {
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetsloggMelding ->
            rapidsConnection.publish(
                JsonMessage.newMessage(
                    eventName = aktivitetsloggMelding.eventNavn,
                    map = aktivitetsloggMelding.innhold,
                ).toJson(),
            )
        }
    }
}
