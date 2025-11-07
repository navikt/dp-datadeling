package no.nav.dagpenger.søknad.modell

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import java.time.LocalDateTime

internal abstract class SøknadMelding(
    packet: JsonMessage,
) {
    val ident = packet["fødselsnummer"].asText()
    val journalpostId: String = packet["journalpostId"].asText()
    val skjemaKode = packet["skjemaKode"].asText()
    val søknadsType = Søknad.SøknadsType.valueOf(packet["type"].asText())
    val datoRegistrert: LocalDateTime = packet["datoRegistrert"].asLocalDateTime()
    abstract val søknadId: String?
    abstract val kanal: Søknad.Kanal
}
