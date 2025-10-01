package no.nav.dagpenger.datadeling.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.datadeling.db.BehandlingRepository
import no.nav.dagpenger.datadeling.db.VedtakRepository
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DagpengerVedtakMottakTest {
    private val testRapid = TestRapid()
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)

    init {
        DagpengerVedtakMottak(
            testRapid,
            behandlingRepository,
            vedtakRepository,
        )
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `skal motta behandlingsresultat og lagre vedtak`() {
        val ident = "12345678903"
        val behandlingId = UUID.randomUUID().toString()
        val sakId = UUID.randomUUID().toString()
        val fraOgMed1 = LocalDate.now().minusDays(10)
        val tilOgMed1 = LocalDate.now()
        val fraOgMed2 = LocalDate.now().plusDays(1)

        every { behandlingRepository.hentSakIdForBehandlingId(eq(behandlingId)) } returns sakId

        val behandlingsresultat =
            """
            {
              "@event_name": "behandlingsresultat",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "datatype": "string",
                "id": "string",
                "type": "Søknad"
              },
              "basertPå": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "automatisk": true,
              "ident": "$ident",
              "opplysninger": [ ],
              "rettighetsperioder": [
                {
                  "fraOgMed": "$fraOgMed1",
                  "tilOgMed": "$tilOgMed1",
                  "harRett": true,
                  "opprinnelse": "Ny"
                },
                {
                  "fraOgMed": "$fraOgMed2",
                  "harRett": false,
                  "opprinnelse": "Ny"
                }
              ]
            }
            """.trimIndent()

        testRapid.sendTestMessage(behandlingsresultat)

        verify {
            vedtakRepository.lagreVedtak(
                eq("$behandlingId-0"),
                eq(ident),
                eq(sakId),
                eq(Vedtak.Utfall.INNVILGET),
                eq(StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER),
                eq(fraOgMed1),
                eq(tilOgMed1),
                null,
                null,
                eq(Vedtak.Kilde.DP),
            )
        }

        verify {
            vedtakRepository.lagreVedtak(
                eq("$behandlingId-1"),
                eq(ident),
                eq(sakId),
                eq(Vedtak.Utfall.AVSLÅTT),
                eq(StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER),
                eq(fraOgMed2),
                null,
                null,
                null,
                eq(Vedtak.Kilde.DP),
            )
        }
    }
}
