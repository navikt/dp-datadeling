package no.nav.dagpenger.datadeling.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

internal class QuizSøknadMelding(
    packet: JsonMessage,
) : SøknadMelding(packet) {
    companion object {
        const val SØKNAD_ID_NØKKEL = "søknadsData.søknad_uuid"
    }

    override val søknadId = packet[SØKNAD_ID_NØKKEL].asText()
    override val kanal = Søknad.Kanal.Digital
}
