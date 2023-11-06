package no.nav.dagpenger.datadeling

import no.nav.dagpenger.datadeling.ressurs.Ressurs
import no.nav.dagpenger.datadeling.ressurs.RessursStatus
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import java.time.LocalDate
import java.util.UUID


internal const val FNR = "01020312342"

internal fun enDatadelingRequest(periode: ClosedRange<LocalDate>, fnr: String = FNR) =
    enDatadelingRequest(
        fraOgMed = periode.start,
        tilOgMed = periode.endInclusive,
        fnr = fnr,
    )

internal fun enDatadelingRequest(
    fraOgMed: LocalDate = 1.januar(),
    tilOgMed: LocalDate? = null,
    fnr: String = FNR
) =
    DatadelingRequest(
        personIdent = fnr,
        fraOgMedDato = fraOgMed,
        tilOgMedDato = tilOgMed,
    )

internal fun enRessurs(
    uuid: UUID = UUID.randomUUID(),
    status: RessursStatus = RessursStatus.OPPRETTET,
    data: DatadelingResponse? = null,
) =
    Ressurs(
        uuid = uuid,
        status = status,
        response = data,
    )

internal fun enDatadelingResponse(vararg perioder: Periode, fnr: String = FNR) =
    DatadelingResponse(
        personIdent = fnr,
        perioder = perioder.asList(),
    )

internal fun emptyResponse() = DatadelingResponse(FNR, emptyList())

internal fun enPeriode(
    periode: ClosedRange<LocalDate>,
    ytelseType: StønadType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
    gjenståendeDager: Int = 100,
) = enPeriode(
    fraOgMed = periode.start,
    tilOgMed = periode.endInclusive,
    ytelseType = ytelseType,
    gjenståendeDager = gjenståendeDager,
)

internal fun enPeriode(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
    ytelseType: StønadType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
    gjenståendeDager: Int = 100,
) = Periode(
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
    ytelseType = ytelseType,
    gjenståendeDager = gjenståendeDager,
)

internal fun Int.januar(year: Int = 2023) = LocalDate.of(year, 1, this)