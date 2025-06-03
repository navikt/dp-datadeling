package no.nav.dagpenger.datadeling.service

import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class SøknaderService(
    private val søknadRepository: SøknadRepository = SøknadRepository(),
) {
    fun hentSoknader(request: DatadelingRequest): List<Søknad> =
        søknadRepository.hentSøknaderFor(
            request.personIdent,
            request.fraOgMedDato,
            request.tilOgMedDato,
        )
}
