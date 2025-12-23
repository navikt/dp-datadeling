package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import org.junit.jupiter.api.Test

class BehandlingResultatTolkerFactoryTest {
    private val factory = standardTolkerFactory
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `skal bruke V1Tolker for eksisterende behandlingsresultater`() {
        val tolker = factory.hentTolker(objectMapper.readTree(innvilgelse_v1))

        tolker.shouldBeInstanceOf<BehandlingResultatV1Tolker>()
        tolker.ident shouldBe "17373649758"
    }
}
