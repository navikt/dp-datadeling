package dp.datadeling.api

import com.github.tomakehurst.wiremock.client.WireMock
import dp.datadeling.logic.process
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicV1Test : TestBase() {

    private val fnr = "01020312342"

    private val emptyResponse = DatadelingResponse(
        personIdent = fnr,
        perioder = emptyList()
    )

    @Test
    fun shouldReturnNoPeriodsIfThereAreNoPeriods() = setUpTestApplication {
        setUpMock(emptyResponse, emptyResponse)

        val response = process(DatadelingRequest(personIdent = fnr, fraOgMedDato = LocalDate.now()))

        assertEquals(emptyList(), response.perioder)
    }

    @Test
    fun shouldReturnOnePeriodIfThereIsOnePeriod() = setUpTestApplication {
        val perioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now().plusDays(5),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )

        setUpMock(
            DatadelingResponse(
                personIdent = fnr,
                perioder = perioder
            ),
            emptyResponse
        )

        val response = process(DatadelingRequest(personIdent = fnr, fraOgMedDato = LocalDate.now()))

        assertEquals(perioder, response.perioder)
    }

    @Test
    fun shouldMergePeriodsWithConsecutiveDates() = setUpTestApplication {
        val fraOgMed = LocalDate.now()
        val tilOgMed = LocalDate.now().plusDays(10)
        val perioder = listOf(
            Periode(
                fraOgMedDato = fraOgMed,
                tilOgMedDato = LocalDate.now().plusDays(5),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            ),
            Periode(
                fraOgMedDato = LocalDate.now().plusDays(6),
                tilOgMedDato = tilOgMed,
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )

        setUpMock(
            DatadelingResponse(
                personIdent = fnr,
                perioder = perioder
            ),
            emptyResponse
        )

        val response = process(DatadelingRequest(personIdent = fnr, fraOgMedDato = fraOgMed))

        assertEquals(1, response.perioder.size)
        assertEquals(fraOgMed, response.perioder[0].fraOgMedDato)
        assertEquals(tilOgMed, response.perioder[0].tilOgMedDato)
    }

    @Test
    fun shouldNotMergePeriodsWithoutConsecutiveDates() = setUpTestApplication {
        val perioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now().plusDays(5),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            ),
            Periode(
                fraOgMedDato = LocalDate.now().plusDays(7),
                tilOgMedDato = LocalDate.now().plusDays(10),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )

        setUpMock(
            DatadelingResponse(
                personIdent = fnr,
                perioder = perioder
            ),
            emptyResponse
        )

        val response = process(DatadelingRequest(personIdent = fnr, fraOgMedDato = LocalDate.now()))

        assertEquals(perioder, response.perioder)
    }
}
