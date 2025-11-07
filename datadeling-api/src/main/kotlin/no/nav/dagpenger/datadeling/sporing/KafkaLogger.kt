package no.nav.dagpenger.datadeling.sporing

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper

internal class KafkaLogger(
    private val rapidsConnection: RapidsConnection,
) : AuditLog() {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()

    override fun audit(hendelse: AuditHendelse) {
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetsloggMelding ->
            val json =
                JsonMessage
                    .newMessage(
                        eventName = aktivitetsloggMelding.eventNavn,
                        map = aktivitetsloggMelding.innhold,
                    ).toJson()

            rapidsConnection.publish(
                hendelse.ident(),
                json,
            )
        }
    }
}
