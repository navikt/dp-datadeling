package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.datadeling.db.SøknadRepository
import no.nav.dagpenger.datadeling.model.Søknad
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test

class SøknadMottakTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>()

    init {
        every { søknadRepository.lagreSøknad(any(), any(), any(), any(), any(), any(), any()) } returns 1

        SøknadMottak(testRapid, søknadRepository)
    }

    @BeforeEach
    internal fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta søknader`() {
        testRapid.sendTestMessage(søknadJson)

        verify(exactly = 1) {
            søknadRepository.lagreSøknad(
                eq("01020312345"),
                eq("123"),
                eq("111"),
                eq("NAV 03-102.23"),
                eq(Søknad.SøknadsType.NySøknad),
                eq(Søknad.Kanal.Digital),
                eq(LocalDateTime.parse("2025-05-21T15:03:30.038191900")),
            )
        }
    }

    @Test
    fun `vi kan motta papirsøknad`() {
        testRapid.sendTestMessage(papirsøknadJson)

        verify(exactly = 1) {
            søknadRepository.lagreSøknad(
                eq("01020312346"),
                null,
                eq("222"),
                eq("NAV 03-102.24"),
                eq(Søknad.SøknadsType.NySøknad),
                eq(Søknad.Kanal.Papir),
                eq(LocalDateTime.parse("2021-01-01T01:01:01.000001")),
            )
        }
    }

    @Test
    fun `vi kan motta søknad fra ny søknadssdialog (quiz-format)`() {
        testRapid.sendTestMessage(søknadJsonFraNyQuiz)

        verify(exactly = 1) {
            søknadRepository.lagreSøknad(
                eq("01020312347"),
                eq("4c4e3f15-ab4b-4de8-8950-773684e1ad59"),
                eq("333"),
                eq("NAV 03-102.25"),
                eq(Søknad.SøknadsType.NySøknad),
                eq(Søknad.Kanal.Digital),
                eq(LocalDateTime.parse("2025-05-21T15:04:33.467787600")),
            )
        }
    }
}

private fun MeterRegistry.getSampleValue(
    name: String,
    vararg labels: Pair<String, String>,
): Double? =
    find(name)
        .tags(*labels.flatMap { listOf(it.first, it.second) }.toTypedArray())
        .timer()
        ?.totalTime(java.util.concurrent.TimeUnit.SECONDS)

private const val SYNTHETIC_DELAY_SECONDS: Long = 5

@Language("JSON")
private val søknadJson =
    """
    {
      "@event_name": "innsending_mottatt",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "01020312345",
      "journalpostId": "111",
      "skjemaKode": "NAV 03-102.23",
      "tittel": "Tittel",
      "type": "NySøknad",
      "datoRegistrert": "2025-05-21T15:03:30.038191900",
      "søknadsData": {
        "brukerBehandlingId": "123",
        "vedlegg": [],
        "skjemaNummer": "NAV12"
      }
    }
    """.trimIndent()

@Language("JSON")
private val papirsøknadJson =
    """
    {
      "@id": "123",
      "@opprettet": "2021-01-01T01:01:01.000001",
      "journalpostId": "222",
      "datoRegistrert": "2021-01-01T01:01:01.000001",
      "skjemaKode": "NAV 03-102.24",
      "tittel": "Tittel",
      "type": "NySøknad",
      "fødselsnummer": "01020312346",
      "aktørId": "1234455",
      "søknadsData": {},
      "@event_name": "innsending_mottatt",
      "system_read_count": 0
    }
    """.trimIndent()

@Language("JSON")
private val søknadJsonFraNyQuiz =
    """
    {
      "@event_name": "innsending_mottatt",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "01020312347",
      "journalpostId": "333",
      "skjemaKode": "NAV 03-102.25",
      "tittel": "Tittel",
      "type": "NySøknad",
      "datoRegistrert": "2025-05-21T15:04:33.467787600",
      "søknadsData": {
        "søknad_uuid": "4c4e3f15-ab4b-4de8-8950-773684e1ad59"
      }
    }
    """.trimIndent()
