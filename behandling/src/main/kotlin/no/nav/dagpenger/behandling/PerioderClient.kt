package no.nav.dagpenger.behandling

import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import java.time.LocalDate

enum class Fagsystem {
    ARENA,
    DP_SAK,
}

enum class YtelseType {
    Ordinær,
    Permittering,
    Fiskeindustri,
}

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val ytelseType: YtelseType,
    val kilde: Fagsystem,
)

interface PerioderClient {
    suspend fun hentDagpengeperioder(request: DatadelingRequestDTO): List<Periode>
}
