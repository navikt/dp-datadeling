package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.datadeling.db.BehandlingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakFattetUtenforArenaMottakTest {
    private val testRapid = TestRapid()
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)

    init {
        VedtakFattetUtenforArenaMottak(testRapid, behandlingRepository)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `skal OpprettMeldekortJob`() {
        val behandlingId = UUID.randomUUID().toString()
        val søknadId = UUID.randomUUID().toString()
        val ident = "01020312345"
        val sakId = UUID.randomUUID().toString()

        testRapid.sendTestMessage(
            """
            {
              "@event_name": "vedtak_fattet_utenfor_arena",
              "behandlingId": "$behandlingId",
              "søknadId": "$søknadId",
              "ident": "$ident",
              "sakId": "$sakId"
            }
            """.trimIndent(),
        )

        verify(exactly = 1) {
            behandlingRepository.lagreData(
                eq(behandlingId),
                eq(søknadId),
                eq(ident),
                eq(sakId),
            )
        }
    }
}
