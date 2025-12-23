package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class BehandlingResultatTolkerFactoryTest {
    private val factory = standardTolkerFactory
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `skal bruke V1Tolker for eksisterende behandlingsresultater`() {
        val json =
            """
            {
              "@event_name": "behandlingsresultat",
              "behandlingId": "019a496c-15f0-7c2f-a645-4bca140706c0",
              "behandlingskjedeId": "019a496c-15f0-7c2f-a645-4bca140706c0",
              "ident": "12345678910",
              "@opprettet": "2025-11-03T12:13:40.100308",
              "opplysninger": [],
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-01-01",
                  "harRett": true,
                  "opprinnelse": "Ny"
                }
              ]
            }
            """.trimIndent()

        val tolker = factory.hentTolker(objectMapper.readTree(json))

        tolker.shouldBeInstanceOf<BehandlingResultatV1Tolker>()
        tolker.ident shouldBe "12345678910"
    }

    @Test
    fun `skal håndtere behandlingsresultat uten opprettet tidspunkt`() {
        val json =
            """
            {
              "@event_name": "behandlingsresultat",
              "behandlingId": "019a496c-15f0-7c2f-a645-4bca140706c0",
              "behandlingskjedeId": "019a496c-15f0-7c2f-a645-4bca140706c0",
              "ident": "12345678910",
              "opplysninger": [],
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-01-01",
                  "harRett": true
                }
              ]
            }
            """.trimIndent()

        val tolker = factory.hentTolker(objectMapper.readTree(json))

        tolker.shouldBeInstanceOf<BehandlingResultatV1Tolker>()
    }
}
