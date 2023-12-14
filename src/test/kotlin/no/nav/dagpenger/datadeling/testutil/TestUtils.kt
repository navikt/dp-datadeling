package no.nav.dagpenger.datadeling.testutil

import no.nav.dagpenger.datadeling.api.perioder.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER
import java.time.LocalDate
import java.util.UUID

internal const val FNR = "01020312342"

internal fun enDatadelingRequest(
    periode: ClosedRange<LocalDate>,
    fnr: String = FNR,
) = enDatadelingRequest(
    fraOgMed = periode.start,
    tilOgMed = periode.endInclusive,
    fnr = fnr,
)

internal fun enDatadelingRequest(
    fraOgMed: LocalDate = 1.januar(),
    tilOgMed: LocalDate? = null,
    fnr: String = FNR,
) = DatadelingRequest(
    personIdent = fnr,
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
)

internal fun enRessurs(
    uuid: UUID = UUID.randomUUID(),
    status: RessursStatus = RessursStatus.OPPRETTET,
    data: DatadelingResponse? = null,
) = Ressurs(
    uuid = uuid,
    status = status,
    response = data,
)

internal fun enDatadelingResponse(
    vararg perioder: Periode,
    fnr: String = FNR,
) = DatadelingResponse(
    personIdent = fnr,
    perioder = perioder.asList(),
)

internal fun emptyResponse() = DatadelingResponse(FNR, emptyList())

internal fun enPeriode(
    periode: ClosedRange<LocalDate>,
    ytelseType: StønadType = DAGPENGER_ARBEIDSSOKER_ORDINAER,
) = enPeriode(
    fraOgMed = periode.start,
    tilOgMed = periode.endInclusive,
    ytelseType = ytelseType,
)

internal fun enPeriode(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
    ytelseType: StønadType = DAGPENGER_ARBEIDSSOKER_ORDINAER,
) = Periode(
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
    ytelseType = ytelseType,
)

internal fun Int.januar(year: Int = 2023) = LocalDate.of(year, 1, this)
