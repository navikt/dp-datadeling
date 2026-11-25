package no.nav.dagpenger.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class SøknadsbehandlingMottakTest {
    private val testRapid = TestRapid()
    private val repository = mockk<SøknadsbehandlingRepository>(relaxed = true)
    private val søknadId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val behandlingId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val sakId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val ident = "12345678901"
    private val tidspunkt = LocalDateTime.parse("2026-11-25T10:51:12.696482")

    init {
        SøknadsbehandlingOpprettetMottak(testRapid, repository)
        SøknadsbehandlingFerdigMottak(testRapid, repository)
        SøknadsbehandlingAvbruttMottak(testRapid, repository)
    }

    @Test
    fun `opprettet lagrer søknadsbehandling med behandlingsskjedeId som sakId`() {
        testRapid.sendTestMessage(melding("søknadsbehandling_opprettet"))

        verify(exactly = 1) { repository.lagreOpprettet(søknadId, behandlingId, sakId, ident, tidspunkt) }
    }

    @Test
    fun `ferdig setter tilstand Ferdig`() {
        testRapid.sendTestMessage(melding("søknadsbehandling_ferdig", """, "førteTil": "Innvilgelse", "rettighetsperioder": []"""))

        verify(
            exactly = 1,
        ) { repository.lagreTilstand(søknadId, behandlingId, sakId, ident, SøknadsbehandlingTilstand.Ferdig, tidspunkt, "Innvilgelse") }
    }

    @Test
    fun `avbrutt setter tilstand Avbrutt`() {
        testRapid.sendTestMessage(melding("søknadsbehandling_avbrutt", """, "årsak": "Trukket""""))

        verify(
            exactly = 1,
        ) { repository.lagreTilstand(søknadId, behandlingId, sakId, ident, SøknadsbehandlingTilstand.Avbrutt, tidspunkt, null) }
    }

    @Test
    fun `melding uten behandlingsskjedeId ignoreres`() {
        testRapid.sendTestMessage(melding("søknadsbehandling_opprettet").replace("behandlingsskjedeId", "behandlingskjedeId"))

        verify(exactly = 0) { repository.lagreOpprettet(any(), any(), any(), any(), any()) }
    }

    @Language("JSON")
    private fun melding(
        event: String,
        ekstra: String = "",
    ) = """
        {
          "@event_name": "$event",
          "@id": "b690b44c-1520-4a2b-9123-bf602bf1a5f6",
          "@opprettet": "2026-11-25T10:51:12.696482",
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "behandlingsskjedeId": "$sakId",
          "søknadId": "$søknadId"$ekstra
        }
        """.trimIndent()
}
