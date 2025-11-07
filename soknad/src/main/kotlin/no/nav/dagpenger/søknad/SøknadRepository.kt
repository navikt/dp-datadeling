package no.nav.dagpenger.søknad

import no.nav.dagpenger.søknad.modell.Søknad
import java.time.LocalDate
import java.time.LocalDateTime

interface SøknadRepository {
    fun lagre(
        ident: String,
        søknadId: String?,
        journalpostId: String,
        skjemaKode: String?,
        søknadsType: Søknad.SøknadsType,
        kanal: Søknad.Kanal,
        datoInnsendt: LocalDateTime,
    ): Int

    fun hentSøknaderFor(
        ident: String,
        fom: LocalDate? = null,
        tom: LocalDate? = null,
        type: List<Søknad.SøknadsType> = emptyList(),
        kanal: List<Søknad.Kanal> = emptyList(),
        offset: Int = 0,
        limit: Int = 20,
    ): List<Søknad>

    fun hentSisteSøknad(ident: String): Søknad?
}
