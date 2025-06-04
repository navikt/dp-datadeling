package no.nav.dagpenger.datadeling.model

import no.nav.dagpenger.kontrakter.felles.StønadType
import java.time.LocalDate

data class Vedtak(
    val vedtakId: String,
    val fagsakId: String,
    val utfall: Utfall,
    val stønadType: StønadType,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate? = null,
    val dagsats: Int? = null,
    val barnetillegg: Int? = null,
) {
    enum class Utfall {
        INNVILGET,
        AVSLÅTT,
    }
}
