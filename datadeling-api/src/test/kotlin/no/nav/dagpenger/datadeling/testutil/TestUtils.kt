package no.nav.dagpenger.datadeling.testutil

import no.nav.dagpenger.datadeling.api.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
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
) = DatadelingRequestDTO(
    personIdent = fnr,
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
)

internal fun enRessurs(
    uuid: UUID = UUID.randomUUID(),
    status: RessursStatus = RessursStatus.OPPRETTET,
    request: DatadelingRequestDTO = enDatadelingRequest(),
    data: DatadelingResponseDTO? = null,
) = Ressurs(
    uuid = uuid,
    status = status,
    request = request,
    response = data,
)

internal fun enDatadelingResponse(
    vararg perioder: PeriodeDTO,
    fnr: String = FNR,
) = DatadelingResponseDTO(
    personIdent = fnr,
    perioder = perioder.asList(),
)

internal fun emptyResponse() = DatadelingResponseDTO(FNR, emptyList())

internal fun enPeriode(
    periode: ClosedRange<LocalDate>,
    ytelseType: YtelseTypeDTO = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
) = enPeriode(
    fraOgMed = periode.start,
    tilOgMed = periode.endInclusive,
    ytelseType = ytelseType,
)

internal fun enPeriode(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
    ytelseType: YtelseTypeDTO = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
) = PeriodeDTO(
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
    ytelseType = ytelseType,
)

internal fun Int.januar(year: Int = 2023) = LocalDate.of(year, 1, this)

internal fun Int.oktober(year: Int = 2023) = LocalDate.of(year, 1, this)
