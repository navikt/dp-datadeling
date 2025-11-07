package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.util.UUID

interface BehandlingResultat {
    val ident: String
    val behandlingId: UUID
    val rettighetsperioder: List<Rettighetsperiode>
    val rettighetstyper: List<Rettighetstyper>
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

enum class Rettighetstype {
    ORDINÆR,
    PERMITTERING,
    LØNNSGARANTI,
    FISK,
}
