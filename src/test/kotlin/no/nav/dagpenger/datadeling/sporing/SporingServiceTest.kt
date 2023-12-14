package no.nav.dagpenger.datadeling.sporing

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class SporingServiceTest {
    private val rapidsConnection = TestRapid()

    @Test
    fun `test something`() {
        SporingService(rapidsConnection).let {
            val hendelse = PersonOpprettetHendelse("123", "123")
            it.hubba(hendelse)

            rapidsConnection.inspektør.message(0).also {
                it["aktiviteter"].size() shouldBe 1
            }
        }
    }
}
