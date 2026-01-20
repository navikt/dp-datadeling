package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.util.UUID

interface BehandlingResultat {
    val ident: String
    val behandlingId: UUID
    val rettighetsperioder: List<Rettighetsperiode>
    val rettighetstyper: List<Rettighetstyper>
    val beregninger: List<BeregnetDag>
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
}

enum class Rettighetstype {
    ORDINÆR,
    PERMITTERING,
    LØNNSGARANTI,
    FISK,
}
