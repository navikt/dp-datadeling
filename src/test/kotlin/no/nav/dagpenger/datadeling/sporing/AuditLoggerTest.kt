package no.nav.dagpenger.datadeling.sporing

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class AuditLoggerTest {
    private val rapidsConnection = TestRapid()

    @Test
    fun `Sender hendelse på rapid`() {
        KafkaAuditLogger(rapidsConnection).let {
            val hendelse = DagpengerPeriodeSpørringHendelse("fnr", "orgnummer")
            it.log(hendelse)

            rapidsConnection.inspektør.message(0).also {
                it["aktiviteter"].size() shouldBe 1
            }
        }
    }
}
