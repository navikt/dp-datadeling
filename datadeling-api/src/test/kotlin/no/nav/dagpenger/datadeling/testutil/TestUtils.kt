package no.nav.dagpenger.datadeling.testutil

import no.nav.dagpenger.datadeling.api.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseAfpDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.FagsystemDTO
import no.nav.dagpenger.datadeling.models.PeriodeAfpDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.dato.januar
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
    fraOgMed: LocalDate = 1.januar(2023),
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
    data: DatadelingResponseAfpDTO? = null,
) = Ressurs(
    uuid = uuid,
    status = status,
    request = request,
    response = data,
)

internal fun enDatadelingAfpResponse(
    vararg perioder: PeriodeAfpDTO,
    fnr: String = FNR,
) = DatadelingResponseAfpDTO(
    personIdent = fnr,
    perioder = perioder.asList(),
)

internal fun enDatadelingResponse(
    vararg perioder: PeriodeDTO,
    fnr: String = FNR,
) = DatadelingResponseDTO(
    personIdent = fnr,
    perioder = perioder.asList(),
)

internal fun enAfpPeriode(
    periode: ClosedRange<LocalDate>,
    ytelseType: YtelseTypeDTO = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
) = enAfpPeriode(
    fraOgMed = periode.start,
    tilOgMed = periode.endInclusive,
    ytelseType = ytelseType,
)

internal fun enAfpPeriode(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
    ytelseType: YtelseTypeDTO = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
    kilde: FagsystemDTO = FagsystemDTO.ARENA,
) = PeriodeAfpDTO(
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
    ytelseType = ytelseType,
)

internal fun enPeriode(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
    ytelseType: YtelseTypeDTO = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
    kilde: FagsystemDTO = FagsystemDTO.ARENA,
) = PeriodeDTO(
    fraOgMedDato = fraOgMed,
    tilOgMedDato = tilOgMed,
    ytelseType = ytelseType,
    kilde = kilde,
)
