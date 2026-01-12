package no.nav.dagpenger.datadeling.sporing

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.datadeling.testutil.FNR
import no.nav.dagpenger.datadeling.testutil.enAfpPeriode
import no.nav.dagpenger.datadeling.testutil.enDatadelingAfpResponse
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enRessurs
import no.nav.dagpenger.dato.januar
import org.junit.jupiter.api.Test

class KafkaLoggerTest {
    private val rapidsConnection = TestRapid()

    @Test
    fun `Sender hendelse på rapid`() {
        val ident = "01020312342"
        KafkaLogger(
            rapidsConnection,
        ).let {
            it.log(DagpengerPeriodeSpørringHendelse(ident, "999888777"))
            it.log(
                DagpengerPeriodeHentetHendelse(
                    "999888777",
                    ressurs =
                        enRessurs(
                            request =
                                enDatadelingRequest(
                                    fnr = FNR,
                                    fraOgMed = 1.januar(2023),
                                ),
                            data =
                                enDatadelingAfpResponse(
                                    enAfpPeriode(periode = 1.januar(2021)..1.januar(2022)),
                                    enAfpPeriode(periode = 1.januar(2022)..1.januar(2023)),
                                ),
                        ),
                ),
            )
        }
        with(rapidsConnection.inspektør) {
            size shouldBe 2
            key(0) shouldBe ident
            message(0)["@event_name"].asText() shouldBe "aktivitetslogg"
            message(0)["hendelse"]["type"].asText() shouldBe "DagpengerPeriodeSpørringHendelse"
            message(0)["ident"].asText() shouldBe ident

            key(1) shouldBe ident
            message(1)["@event_name"].asText() shouldBe "aktivitetslogg"
            message(1)["hendelse"]["type"].asText() shouldBe "DagpengerPeriodeHentetHendelse"
            message(1)["ident"].asText() shouldBe ident
        }
    }
}
