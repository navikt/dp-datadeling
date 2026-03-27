package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.datadeling.models.DagpengestatusRequestDTO
import java.time.LocalDate
import kotlin.test.Test

class DagpengestatusServiceTest {
    private val repository: BehandlingResultatRepository = mockk()
    private val service = DagpengestatusService(DagpengestatusRepository(repository))

    @Test
    fun `returnerer første innvilgelse-dato fra behandlingsresultat`() {
        every { repository.hent("12345678901") } returns
            listOf(lagInnvilgelseJson("2026-03-15"))

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat shouldNotBe null
        resultat!!.forsteDagpengevedtakDato shouldBe LocalDate.of(2026, 3, 15)
    }

    @Test
    fun `returnerer null når ingen behandlingsresultat finnes`() {
        every { repository.hent("12345678901") } returns emptyList()

        service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901")) shouldBe null
    }

    @Test
    fun `ignorerer avslag`() {
        every { repository.hent("12345678901") } returns
            listOf(lagAvslagJson("2026-01-01"))

        service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901")) shouldBe null
    }

    @Test
    fun `returnerer tidligste dato blant flere innvilgelser`() {
        every { repository.hent("12345678901") } returns
            listOf(
                lagInnvilgelseJson("2026-05-01"),
                lagInnvilgelseJson("2026-03-15"),
            )

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat shouldNotBe null
        resultat!!.forsteDagpengevedtakDato shouldBe LocalDate.of(2026, 3, 15)
    }

    @Test
    fun `returnerer innvilgelse selv om avslag også finnes`() {
        every { repository.hent("12345678901") } returns
            listOf(
                lagAvslagJson("2026-01-01"),
                lagInnvilgelseJson("2026-03-15"),
            )

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat shouldNotBe null
        resultat!!.forsteDagpengevedtakDato shouldBe LocalDate.of(2026, 3, 15)
    }

    private val testObjectMapper = jacksonObjectMapper()

    private fun lagInnvilgelseJson(fraOgMed: String) = testObjectMapper.readTree(lagBehandlingsresultatJson("Innvilgelse", fraOgMed, true))

    private fun lagAvslagJson(fraOgMed: String) = testObjectMapper.readTree(lagBehandlingsresultatJson("Avslag", fraOgMed, false))

    private fun lagBehandlingsresultatJson(
        førteTil: String,
        fraOgMed: String,
        harRett: Boolean,
    ) = //language=JSON
        """
        {
            "behandlingId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
            "behandletHendelse": {"datatype": "UUID", "id": "019b4a51-6ef8-7714-8f5f-924a23137d03", "type": "Søknad", "skjedde": "2026-03-15"},
            "behandlingskjedeId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
            "automatisk": true,
            "ident": "12345678901",
            "rettighetsperioder": [{"fraOgMed": "$fraOgMed", "harRett": $harRett}],
            "opprettet": "2026-03-15T10:00:00",
            "sistEndret": "2026-03-15T10:00:00",
            "opplysninger": [],
            "utbetalinger": [],
            "behandletAv": [],
            "førteTil": "$førteTil"
        }
        """.trimIndent()
}
