package no.nav.dagpenger.datadeling.model

import java.time.LocalDateTime

data class Vedtak(
    val vedtakId: String,
    val fagsakId: String,
    val status: Status,
    val datoFattet: LocalDateTime,
    val fraDato: LocalDateTime,
    val tilDato: LocalDateTime? = null,
) {
    enum class Status {
        INNVILGET,
        AVSLÅTT,
        STANS,
        ENDRING,
    }
}
