package no.nav.dagpenger.søknad.modell

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import java.time.LocalDateTime

internal abstract class SøknadMelding(
    packet: JsonMessage,
) {
    val ident = packet["fødselsnummer"].asString()
    val journalpostId: String = packet["journalpostId"].asString()
    val skjemaKode = packet["skjemaKode"].asString()
    val søknadsType = Søknad.SøknadsType.valueOf(packet["type"].asString())
    val datoRegistrert: LocalDateTime = packet["datoRegistrert"].asLocalDateTime()
    abstract val søknadId: String?
    abstract val kanal: Søknad.Kanal
}
