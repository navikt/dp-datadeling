package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.meldekortBeregning_v1
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.stans_v1
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
    fun `tar bare med rettighetsperioder som er true`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(stans_v1)
        val tolker = BehandlingResultatV1Tolker.fra(behandlingResultat)

        tolker.behandlingId shouldBe UUID.fromString("019b4a54-0d82-7a74-b0aa-a19d160016f8")
        tolker.ident shouldBe "10399847102"

        tolker.rettighetsperioder.size shouldBe 1
        tolker.rettighetsperioder.first().fraOgMed shouldBe LocalDate.of(2018, 6, 21)
        tolker.rettighetsperioder.first().tilOgMed shouldBe LocalDate.of(2018, 7, 21)
        tolker.rettighetsperioder.first().harRett shouldBe true

        tolker.rettighetstyper.size shouldBe 1
        tolker.rettighetstyper.first().type shouldBe Rettighetstype.ORDINÆR
    }

    @Test
    fun `har utbetalinger`() {
        val behandlingResultat: JsonNode = objectMapper.readTree(meldekortBeregning_v1)
        val tolker = BehandlingResultatV1Tolker.fra(behandlingResultat)

        tolker.utbetalinger.size shouldBe 11
        tolker.utbetalinger[0].dato shouldBe LocalDate.of(2018, 6, 21)
        tolker.utbetalinger[0].sats shouldBe 1259
        tolker.utbetalinger[0].utbetaling shouldBe 719

        tolker.utbetalinger[10].dato shouldBe LocalDate.of(2018, 7, 1)
        tolker.utbetalinger[10].sats shouldBe 1259
        tolker.utbetalinger[10].utbetaling shouldBe 0

        tolker.utbetalinger.sumOf { it.utbetaling } shouldBe 5036
    }
}
