package no.nav.dagpenger.søknad

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.søknad.modell.Søknad

class SøknadService(
    private val søknadRepository: SøknadRepository,
) {
    fun hentSøknader(request: DatadelingRequestDTO): List<Søknad> =
        søknadRepository.hentSøknaderFor(
            request.personIdent,
            request.fraOgMedDato,
            request.tilOgMedDato,
        )

    fun hentSisteSøknad(ident: String): Søknad? = søknadRepository.hentSisteSøknad(ident)
}
