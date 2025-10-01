package no.nav.dagpenger.datadeling.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

data class Meldekort(
    val id: String,
    val periode: Periode,
    val dager: List<Dag>,
    val kanSendesFra: LocalDate,
    val opprettetAv: OpprettetAv,
    val migrert: Boolean,
    val begrunnelse: String?,
    val kilde: Kilde,
    val innsendtTidspunkt: LocalDateTime,
    val registrertArbeidssøker: Boolean,
    val meldedato: LocalDate,
) {
    data class Periode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    ) {
        init {
            require(tilOgMed.minusDays(13).isEqual(fraOgMed)) {
                "Perioden må være 14 dager lang"
            }
            require(fraOgMed.isBefore(tilOgMed)) {
                "Fra og med-dato kan ikke være etter til og med-dato"
            }
        }
    }

    data class Dag(
        val dato: LocalDate,
        val aktiviteter: List<Aktivitet> = emptyList(),
        val dagIndex: Int,
        val meldt: Boolean,
    ) {
        init {
            require(aktiviteter.validerIngenDuplikateAktivitetsTyper()) {
                "Duplikate Aktivitetstyper er ikke tillatt i aktivitetslisten"
            }
            require(aktiviteter.validerAktivitetsTypeKombinasjoner()) {
                "Aktivitetene Syk og Arbeid, samt Fravær og Arbeid kan ikke kombineres."
            }
            require(aktiviteter.validerArbeidedeTimer()) {
                "Arbeidede timer kan ikke være null, 0 eller over 24 timer. Kun hele og halve timer er gyldig input"
            }
            require(aktiviteter.validerIngenArbeidedeTimerUtenArbeid()) {
                "Aktiviteter som ikke er arbeid kan ikke ha utfylte arbeidede timer"
            }
        }

        data class Aktivitet(
            val id: UUID,
            val type: Type,
            val timer: String?,
        ) {
            enum class Type {
                Arbeid,
                Syk,
                Utdanning,
                Fravaer,
            }
        }

        private fun List<Aktivitet>.validerIngenDuplikateAktivitetsTyper(): Boolean =
            this
                .map { it.type }
                .toSet()
                .size == this.size

        private fun List<Aktivitet>.validerAktivitetsTypeKombinasjoner(): Boolean =
            this
                .map { it.type }
                .let { typer ->
                    if (typer.contains(Aktivitet.Type.Syk) && typer.contains(Aktivitet.Type.Arbeid)) {
                        false
                    } else if (typer.contains(Aktivitet.Type.Fravaer) && typer.contains(Aktivitet.Type.Arbeid)) {
                        false
                    } else {
                        true
                    }
                }

        private fun List<Aktivitet>.validerArbeidedeTimer(): Boolean =
            this
                .filter { it.type == Aktivitet.Type.Arbeid }
                .all {
                    if (it.timer == null) return false
                    try {
                        val arbeidedeTimer = Duration.parseIsoString(it.timer)
                        val timer = arbeidedeTimer.inWholeHours
                        val minutter = arbeidedeTimer.inWholeMinutes % 60
                        val arbeidedeTimerIMinutter = timer * 60 + minutter
                        val døgnIMinutter = 24L * 60L

                        val erInnenforEttDøgn = arbeidedeTimerIMinutter <= døgnIMinutter
                        val erHelEllerHalvTime = (minutter == 0L || minutter == 30L)
                        val erIkkeNull = timer + minutter != 0L

                        erIkkeNull && erInnenforEttDøgn && erHelEllerHalvTime
                    } catch (e: Exception) {
                        false
                    }
                }

        private fun List<Aktivitet>.validerIngenArbeidedeTimerUtenArbeid(): Boolean =
            this
                .filter { it.type != Aktivitet.Type.Arbeid }
                .all { it.timer == null }
    }

    enum class OpprettetAv {
        Arena,
        Dagpenger,
    }

    data class Kilde(
        val rolle: Rolle,
        val ident: String,
    )

    enum class Rolle {
        Bruker,
        Saksbehandler,
    }
}
