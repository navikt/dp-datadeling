package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.meldekortBeregning_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.stans_v1
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BehandlingResultatV1TolkerTest {
    @Test
    fun `tolke behandlingresultat v1`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(innvilgelse_v1)
        val tolker = BehandlingResultatV1Tolker.fra(behandlingResultat)

        tolker.behandlingId shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        tolker.ident shouldBe "17373649758"

        tolker.rettighetsperioder.size shouldBe 1
        tolker.rettighetsperioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first().tilOgMed shouldBe null
        tolker.rettighetsperioder.first().harRett shouldBe true

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `tar med alle rettighetsperioder`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(stans_v1)
        val tolker = BehandlingResultatV1Tolker.fra(behandlingResultat)

        tolker.behandlingId shouldBe UUID.fromString("019b4a54-0d82-7a74-b0aa-a19d160016f8")
        tolker.ident shouldBe "10399847102"

        tolker.rettighetsperioder.size shouldBe 2
        tolker.rettighetsperioder.first { it.harRett }.fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first { it.harRett }.tilOgMed shouldBe LocalDate.of(2018, 7, 21)
        tolker.rettighetsperioder.first { !it.harRett }.fraOgMed shouldBe LocalDate.of(2018, 7, 22)

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `har utbetalinger`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(meldekortBeregning_v1)
        val tolker = BehandlingResultatV1Tolker.fra(behandlingResultat)

        tolker.beregninger.size shouldBe 11
        tolker.beregninger[0].dato shouldBe LocalDate.of(2018, 6, 21)
        tolker.beregninger[0].sats shouldBe 1259
        tolker.beregninger[0].utbetaling shouldBe 719
        tolker.beregninger[0].gjenståendeDager shouldBe 519

        tolker.beregninger[10].dato shouldBe LocalDate.of(2018, 7, 1)
        tolker.beregninger[10].sats shouldBe 1259
        tolker.beregninger[10].utbetaling shouldBe 0
        tolker.beregninger[10].gjenståendeDager shouldBe 513

        tolker.beregninger.sumOf { it.utbetaling } shouldBe 5036
    }

    @Test
    fun `ordinær er implisitt når rettighetstypeopplysninger mangler`() {
        val tolker = tolk()

        tolker.rettighetstyper.map { Triple(it.type, it.fraOgMed, it.tilOgMed) } shouldBe
            listOf(Triple(Rettighetstype.ORDINÆR, LocalDate.MIN, LocalDate.MAX))
    }

    @Test
    fun `ordinær er implisitt når alle særtyper er false`() {
        val tolker =
            tolk(
                opplysning("0194881f-9444-7a73-a458-0af81c034d86", periode(false)),
                opplysning("0194881f-9444-7a73-a458-0af81c034d87", periode(false)),
                opplysning("0194881f-9444-7a73-a458-0af81c034d88", periode(false)),
            )

        tolker.rettighetstyper.map { Triple(it.type, it.fraOgMed, it.tilOgMed) } shouldBe
            listOf(Triple(Rettighetstype.ORDINÆR, LocalDate.MIN, LocalDate.MAX))
    }

    @ParameterizedTest
    @CsvSource(
        "0194881f-9444-7a73-a458-0af81c034d86, PERMITTERING",
        "0194881f-9444-7a73-a458-0af81c034d87, LØNNSGARANTI",
        "0194881f-9444-7a73-a458-0af81c034d88, FISK",
    )
    fun `sann særtype overstyrer også gammel ordinæropplysning`(
        opplysningTypeId: String,
        forventetType: Rettighetstype,
    ) {
        val tolker =
            tolk(
                opplysning("0194881f-9444-7a73-a458-0af81c034d8a", periode(true)),
                opplysning(opplysningTypeId, periode(true)),
            )

        tolker.rettighetstyper.map { Triple(it.type, it.fraOgMed, it.tilOgMed) } shouldBe
            listOf(Triple(forventetType, LocalDate.MIN, LocalDate.MAX))
    }

    @Test
    fun `ordinær dekker periodene før mellom og etter sanne særtyper`() {
        val tolker =
            tolk(
                opplysning(
                    "0194881f-9444-7a73-a458-0af81c034d86",
                    periode(true, "2026-01-01", "2026-01-10"),
                    periode(false, "2026-01-11"),
                ),
                opplysning("0194881f-9444-7a73-a458-0af81c034d88", periode(true, "2026-01-12", "2026-01-20")),
            )

        tolker.rettighetstyper.map { Triple(it.type, it.fraOgMed, it.tilOgMed) } shouldBe
            listOf(
                Triple(Rettighetstype.ORDINÆR, LocalDate.MIN, LocalDate.of(2025, 12, 31)),
                Triple(Rettighetstype.PERMITTERING, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)),
                Triple(Rettighetstype.ORDINÆR, LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 11)),
                Triple(Rettighetstype.FISK, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 20)),
                Triple(Rettighetstype.ORDINÆR, LocalDate.of(2026, 1, 21), LocalDate.MAX),
            )
    }

    @Test
    fun `overlappende og tilstøtende særtyper gir ikke ordinære perioder mellom seg`() {
        val tolker =
            tolk(
                opplysning("0194881f-9444-7a73-a458-0af81c034d88", periode(true, "2026-01-16", "2026-01-20")),
                opplysning(
                    "0194881f-9444-7a73-a458-0af81c034d86",
                    periode(true, "2026-01-01", "2026-01-15"),
                    periode(true, "2026-01-05", "2026-01-10"),
                ),
            )

        tolker.rettighetstyper
            .filter { it.type == Rettighetstype.ORDINÆR }
            .map { it.fraOgMed to it.tilOgMed } shouldBe
            listOf(
                LocalDate.MIN to LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 21) to LocalDate.MAX,
            )
    }

    @Test
    fun `åpne særtypeperioder avgrenser ordinær til gapet mellom dem`() {
        val tolker =
            tolk(
                opplysning("0194881f-9444-7a73-a458-0af81c034d86", periode(true, tilOgMed = "2026-01-10")),
                opplysning("0194881f-9444-7a73-a458-0af81c034d87", periode(true, fraOgMed = "2026-01-12")),
            )

        tolker.rettighetstyper.map { Triple(it.type, it.fraOgMed, it.tilOgMed) } shouldBe
            listOf(
                Triple(Rettighetstype.PERMITTERING, LocalDate.MIN, LocalDate.of(2026, 1, 10)),
                Triple(Rettighetstype.ORDINÆR, LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 11)),
                Triple(Rettighetstype.LØNNSGARANTI, LocalDate.of(2026, 1, 12), LocalDate.MAX),
            )
    }

    private fun tolk(vararg opplysninger: String): BehandlingResultatV1Tolker {
        val json = objectMapper.readTree(innvilgelse_v1) as ObjectNode
        json.put("ident", "12345678901")
        json.set("opplysninger", objectMapper.readTree(opplysninger.joinToString(prefix = "[", postfix = "]")))
        json.set("utbetalinger", objectMapper.createArrayNode())
        return BehandlingResultatV1Tolker.fra(json)
    }

    private fun opplysning(
        opplysningTypeId: String,
        vararg perioder: String,
    ): String =
        """
        {
          "opplysningTypeId": "$opplysningTypeId",
          "navn": "Rettighetstype",
          "datatype": "boolsk",
          "perioder": [${perioder.joinToString()}]
        }
        """.trimIndent()

    private fun periode(
        verdi: Boolean,
        fraOgMed: String? = null,
        tilOgMed: String? = null,
    ): String =
        """
        {
          "id": "00000000-0000-0000-0000-000000000001",
          "opprettet": "2026-01-01T00:00:00",
          "opprinnelse": "Ny",
          "gyldigFraOgMed": ${fraOgMed?.let { "\"$it\"" }},
          "gyldigTilOgMed": ${tilOgMed?.let { "\"$it\"" }},
          "verdi": {"verdi": $verdi, "datatype": "boolsk"}
        }
        """.trimIndent()
}
