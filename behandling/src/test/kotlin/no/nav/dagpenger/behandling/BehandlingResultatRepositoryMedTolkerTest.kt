package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import java.time.LocalDate
import kotlin.test.Test

class BehandlingResultatRepositoryMedTolkerTest {
    private val repository: BehandlingResultatRepository = mockk()
    private val repositoryMedTolker = BehandlingResultatRepositoryMedTolker(repository)

    @Test
    fun `hentDagpengeperioder returnerer kun perioder med harRett`() {
        every { repository.hent("12345678901") } returns
            listOf(objectMapper.readTree(BehandlingsresultatScenarioer.stans_v1))

        val perioder =
            runBlocking {
                repositoryMedTolker.hentDagpengeperioder(
                    DatadelingRequestDTO("12345678901", LocalDate.MIN, LocalDate.MAX),
                )
            }

        perioder.size shouldBe 1
        perioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        perioder.first().tilOgMed shouldBe LocalDate.of(2018, 7, 21)
    }

    @Test
    fun `hentDagpengeperioder returnerer tom liste for avslag`() {
        val avslagJson =
            objectMapper.readTree(
                """
                {
                    "behandlingId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                    "behandletHendelse": {"datatype": "UUID", "id": "019b4a51-6ef8-7714-8f5f-924a23137d03", "type": "Søknad", "skjedde": "2026-03-15"},
                    "behandlingskjedeId": "019b4a51-6ef8-7714-8f5f-924a23137d03",
                    "automatisk": true,
                    "ident": "12345678901",
                    "rettighetsperioder": [{"fraOgMed": "2026-01-01", "harRett": false}],
                    "opprettet": "2026-03-15T10:00:00",
                    "sistEndret": "2026-03-15T10:00:00",
                    "opplysninger": [],
                    "utbetalinger": [],
                    "behandletAv": [],
                    "førteTil": "Avslag"
                }
                """.trimIndent(),
            )

        every { repository.hent("12345678901") } returns listOf(avslagJson)

        val perioder =
            runBlocking {
                repositoryMedTolker.hentDagpengeperioder(
                    DatadelingRequestDTO("12345678901", LocalDate.MIN, LocalDate.MAX),
                )
            }

        perioder.size shouldBe 0
    }
}
