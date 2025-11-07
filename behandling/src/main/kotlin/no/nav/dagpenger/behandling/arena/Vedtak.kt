package no.nav.dagpenger.behandling.arena

import no.nav.dagpenger.datadeling.models.StonadTypeDTO
import java.time.LocalDate

data class Vedtak(
    val vedtakId: String,
    val fagsakId: String,
    val utfall: Utfall,
    val stønadType: StonadTypeDTO,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate? = null,
    val dagsats: Int? = null,
    val barnetillegg: Int? = null,
    val kilde: Kilde = Kilde.ARENA,
) {
    enum class Utfall {
        INNVILGET,
        AVSLÅTT,
    }

    enum class Kilde {
        ARENA,
        DP,
    }
}
