package no.nav.dagpenger.datadeling.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

internal class PapirSøknadsMelding(
    packet: JsonMessage,
) : SøknadMelding(packet) {
    override val søknadId: String?
        get() = null
    override val kanal = Søknad.Kanal.Papir
}
