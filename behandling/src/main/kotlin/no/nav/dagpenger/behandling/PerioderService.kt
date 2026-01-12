package no.nav.dagpenger.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.dato.Datoperiode

class PerioderService(
    vararg kilde: PerioderClient,
) {
    private val kilder: List<PerioderClient> = kilde.asList()

    fun hentDagpengeperioder(request: DatadelingRequestDTO) =
        runBlocking {
            val periode = Datoperiode(request.fraOgMedDato, request.tilOgMedDato)
            val kildeOppslag = kilder.map { client -> async { client.hentDagpengeperioder(request) } }

            val perioder =
                kildeOppslag
                    .awaitAll()
                    .flatten()
                    .filter {
                        val kandidat = Datoperiode(it.fraOgMed, it.tilOgMed)
                        periode.overlapperMed(kandidat)
                    }.sortedBy { it.fraOgMed }

            DatadelingResponseDTO(
                personIdent = request.personIdent,
                perioder =
                    perioder.map {
                        PeriodeDTO(
                            fraOgMedDato = it.fraOgMed,
                            tilOgMedDato = it.tilOgMed,
                            ytelseType = it.ytelseType.tilDTO(),
                            kilde = it.kilde.tilDTO(),
                        )
                    },
            )
        }

    private fun YtelseType.tilDTO() =
        when (this) {
            YtelseType.Ordinær -> YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
            YtelseType.Permittering -> YtelseTypeDTO.DAGPENGER_PERMITTERING_ORDINAER
            YtelseType.Fiskeindustri -> YtelseTypeDTO.DAGPENGER_PERMITTERING_FISKEINDUSTRI
        }

    private fun Fagsystem.tilDTO() =
        when (this) {
            Fagsystem.ARENA -> FagsystemDTO.ARENA
            Fagsystem.DP_SAK -> FagsystemDTO.DP_SAK
        }
}
