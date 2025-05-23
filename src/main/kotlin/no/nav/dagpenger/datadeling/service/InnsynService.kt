package no.nav.dagpenger.datadeling.service

import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest

class InnsynService(
    private val søknadRepository: SøknadRepository = SøknadRepository(),
    private val vedtakRepository: VedtakRepository = VedtakRepository(),
) {
    fun hentSoknader(request: DatadelingRequest): List<Søknad> =
        søknadRepository.hentSøknaderFor(
            request.personIdent,
            request.fraOgMedDato,
            request.tilOgMedDato,
        )

    fun hentVedtak(request: DatadelingRequest): List<Vedtak> =
        vedtakRepository.hentVedtakFor(
            request.personIdent,
            request.fraOgMedDato,
            request.tilOgMedDato,
        )
}
