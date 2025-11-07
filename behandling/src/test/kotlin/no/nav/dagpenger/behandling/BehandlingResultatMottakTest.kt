package no.nav.dagpenger.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class BehandlingResultatMottakTest {
    private val testRapid = TestRapid()
    private val behandlingResultatRepositoryPostgresql = mockk<BehandlingResultatRepository>()
    private val sakIdHenter = mockk<SakIdHenter>()

    init {

        BehandlingResultatMottak(testRapid, sakIdHenter, behandlingResultatRepositoryPostgresql)
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

        coEvery { sakIdHenter.hentSakId(any()) } returns UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
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

        val jsonMelding =
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultat.json")!!
                .reader()
                .readText()
        testRapid.sendTestMessage(jsonMelding)

        verify {
            behandlingResultatRepositoryPostgresql.lagre(any(), any(), any(), any(), any(), any())
        }

        behandlingId.captured shouldBe UUID.fromString("019a496c-15f0-7c2f-a645-4bca140706c0")
        basertPåId.captured shouldBe null
        ident.captured shouldBe "16261111906"
        sakId.captured shouldBe UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        opprettetTidspunkt.captured shouldBe LocalDateTime.of(2025, 11, 3, 12, 13, 40, 100308000)
    }
}
