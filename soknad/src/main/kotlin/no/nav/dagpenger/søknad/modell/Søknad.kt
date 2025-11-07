package no.nav.dagpenger.søknad.modell

import java.time.LocalDateTime

data class Søknad(
    val søknadId: String?,
    val journalpostId: String,
    val skjemaKode: String?,
    val søknadsType: SøknadsType,
    val kanal: Kanal,
    val datoInnsendt: LocalDateTime,
) {
    enum class SøknadsType {
        NySøknad,
        Gjenopptak,
    }

    enum class Kanal {
        Papir,
        Digital,
    }
}
