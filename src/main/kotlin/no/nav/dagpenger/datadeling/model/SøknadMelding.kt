package no.nav.dagpenger.datadeling.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import java.time.LocalDateTime

internal abstract class SøknadMelding(
    packet: JsonMessage,
) {
    internal val ident = packet["fødselsnummer"].asText()
    internal val journalpostId: String = packet["journalpostId"].asText()
    internal val skjemaKode = packet["skjemaKode"].asText()
    internal val søknadsType = Søknad.SøknadsType.valueOf(packet["type"].asText())
    internal val datoRegistrert: LocalDateTime = packet["datoRegistrert"].asLocalDateTime()
    abstract val søknadId: String?
    abstract val kanal: Søknad.Kanal
}
