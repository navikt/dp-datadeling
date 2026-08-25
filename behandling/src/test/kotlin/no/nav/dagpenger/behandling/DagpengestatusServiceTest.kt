package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.datadeling.models.DagpengestatusRequestDTO
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import kotlin.test.Test

class DagpengestatusServiceTest {
    private val repository: BehandlingResultatRepository = mockk()
    private val service = DagpengestatusService(DagpengestatusRepository(repository))

    @Test
    fun `returnerer null dato når ingen behandlingsresultat finnes`() {
        every { repository.hent("12345678901") } returns emptyList()

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat.personIdent shouldBe "12345678901"
        resultat.forsteDato shouldBe null
    }

    @Test
    fun `returnerer tidligste dato blant flere innvilgelser og avslag der første er avslag`() {
        every { repository.hent("12345678901") } returns
            listOf(
                lagInnvilgelseJson("2026-05-01"),
                lagInnvilgelseJson("2026-03-15"),
                lagAvslagJson("2026-04-15"),
                lagAvslagJson("2024-04-15"),
            )

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat.forsteDato shouldBe LocalDate.of(2024, 4, 15)
    }

    @Test
    fun `returnerer tidligste dato blant flere innvilgelser og avslag der første er innvilgelse`() {
        every { repository.hent("12345678901") } returns
            listOf(
                lagInnvilgelseJson("2026-05-01"),
                lagInnvilgelseJson("2026-03-15"),
                lagAvslagJson("2026-04-15"),
                lagInnvilgelseJson("2024-04-15"),
            )

        val resultat = service.hentDagpengestatus(DagpengestatusRequestDTO("12345678901"))

        resultat.forsteDato shouldBe LocalDate.of(2024, 4, 15)
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
            "regelverk" : "Dagpenger",
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
