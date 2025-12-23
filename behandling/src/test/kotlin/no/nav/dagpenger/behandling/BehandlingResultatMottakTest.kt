package no.nav.dagpenger.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.BehandlingsresultatScenarioer.innvilgelse_v1
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class BehandlingResultatMottakTest {
    private val testRapid = TestRapid()
    private val behandlingResultatRepositoryPostgresql = mockk<BehandlingResultatRepository>()

    init {
        BehandlingResultatMottak(testRapid, behandlingResultatRepositoryPostgresql)
    }

    @BeforeEach
    internal fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta behandlingresultat`() {
        val ident = slot<String>()
        val behandlingId = slot<UUID>()
        val basertPåId = slot<UUID?>()
        val sakId = slot<UUID>()
        val json = slot<String>()
        val opprettetTidspunkt = slot<LocalDateTime>()

        every {
            behandlingResultatRepositoryPostgresql.lagre(
                ident = capture(ident),
                behandlingId = capture(behandlingId),
                basertPåId = captureNullable(basertPåId),
                sakId = capture(sakId),
                json = capture(json),
                opprettetTidspunkt = capture(opprettetTidspunkt),
            )
        } returns Unit

        testRapid.sendTestMessage(innvilgelse_v1)

        verify {
            behandlingResultatRepositoryPostgresql.lagre(any(), any(), any(), any(), any(), any())
        }

        behandlingId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        basertPåId.captured shouldBe null
        ident.captured shouldBe "17373649758"
        sakId.captured shouldBe UUID.fromString("019b4a51-6ef8-7714-8f5f-924a23137d03")
        opprettetTidspunkt.captured shouldBe LocalDateTime.parse("2025-12-23T09:26:50.618236")
    }
}
