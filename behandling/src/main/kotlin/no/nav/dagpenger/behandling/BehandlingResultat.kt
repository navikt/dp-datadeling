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

    // Periodiserte opplysninger, sortert på fraOgMed. Tom liste hvis opplysningen mangler.
    val antallBarn: List<Periodisert<Int>> get() = emptyList()
    val dagsats: List<Periodisert<Int>> get() = emptyList()
    val fastsattVanligArbeidstid: List<Periodisert<Double>> get() = emptyList()
}

// fraOgMed er null når verdien gjelder fra ubestemt tid tilbake, tilOgMed er null når den ikke har sluttdato
data class Periodisert<T>(
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val verdi: T,
)

// Ved overlapp gjelder den siste perioden
fun <T> List<Periodisert<T>>.gjelderPå(dato: LocalDate): T? =
    lastOrNull { (it.fraOgMed == null || it.fraOgMed <= dato) && (it.tilOgMed == null || it.tilOgMed >= dato) }?.verdi

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
