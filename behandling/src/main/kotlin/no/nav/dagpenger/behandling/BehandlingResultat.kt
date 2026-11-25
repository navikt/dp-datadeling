package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.util.UUID

interface BehandlingResultat {
    val ident: String
    val behandlingId: UUID
    val rettighetsperioder: List<Rettighetsperiode>
    val rettighetstyper: List<Rettighetstyper>
    val beregninger: List<BeregnetDag>

    // Opplysningene kan mangle i behandlingsresultatet, og teller nedover. Laveste verdi er gjeldende.
    val gjenståendeSanksjon: Int? get() = null
    val gjenståendeTidsbegrensetBortfall: Int? get() = null
}

interface Rettighetsperiode {
    val fraOgMed: LocalDate
    val tilOgMed: LocalDate?
    val harRett: Boolean
}

interface Rettighetstyper {
    val type: Rettighetstype
    val fraOgMed: LocalDate
    val tilOgMed: LocalDate
}

interface BeregnetDag {
    val dato: LocalDate
    val sats: Int
    val utbetaling: Int
    val gjenståendeDager: Int
}

enum class Rettighetstype {
    ORDINÆR,
    PERMITTERING,
    LØNNSGARANTI,
    FISK,
}
