package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.meldekortBeregning_v1
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class BehandlingResultatTolkerFactoryTest {
    private val factory = standardTolkerFactory
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `skal bruke JsonNodeTolker for eksisterende behandlingsresultater`() {
        val tolker = factory.hentTolker(objectMapper.readTree(meldekortBeregning_v1))

        tolker.shouldBeInstanceOf<BehandlingResultatJsonNodeTolker>()
        tolker.ident shouldBe "17373649758"
        tolker.beregninger shouldHaveSize 11
        tolker.beregninger.last().gjenståendeDager shouldBe 513
    }
}
