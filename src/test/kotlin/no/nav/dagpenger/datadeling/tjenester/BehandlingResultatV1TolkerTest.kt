package no.nav.dagpenger.datadeling.tjenester

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.datadeling.db.Rettighetstype
import no.nav.dagpenger.datadeling.objectMapper
import java.time.LocalDate
import kotlin.test.Test

class BehandlingResultatV1TolkerTest {
    @Test
    fun `tolke behandlingresultat v1`() {
        val json =
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultat.json")!!
                .reader()
                .readText()

        val behandlingResultat: JsonNode = objectMapper.readTree(json)
        val tolker = BehandlingResultatV1Tolker(behandlingResultat)

        tolker.behandlingId shouldBe java.util.UUID.fromString("019a496c-15f0-7c2f-a645-4bca140706c0")
        tolker.ident shouldBe "16261111906"

        tolker.rettighetsperioder.size shouldBe 1
        tolker.rettighetsperioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first().tilOgMed shouldBe null
        tolker.rettighetsperioder.first().harRett shouldBe true

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `tar bare med rettighetsperioder som er true`() {
        val json =
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultatMedStans.json")!!
                .reader()
                .readText()

        val behandlingResultat: JsonNode = objectMapper.readTree(json)
        val tolker = BehandlingResultatV1Tolker(behandlingResultat)

        tolker.behandlingId shouldBe java.util.UUID.fromString("019a496c-15f0-7c2f-a645-4bca140706c0")
        tolker.ident shouldBe "16261111906"

        tolker.rettighetsperioder.size shouldBe 1
        tolker.rettighetsperioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first().tilOgMed shouldBe LocalDate.of(2018, 7, 21)
        tolker.rettighetsperioder.first().harRett shouldBe true

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }
}
