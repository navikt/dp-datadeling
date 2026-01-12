package no.nav.dagpenger.dato

import java.time.LocalDate

data class Datoperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = LocalDate.MAX,
) {
    fun overlapperMed(other: Datoperiode): Boolean {
        val tom = tilOgMed ?: LocalDate.MAX
        val otherTom = other.tilOgMed ?: LocalDate.MAX
        return fraOgMed <= otherTom && tom >= other.fraOgMed
    }
}
