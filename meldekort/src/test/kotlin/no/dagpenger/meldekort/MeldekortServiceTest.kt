package no.dagpenger.meldekort

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.MeldekortKildeDTO
import no.nav.dagpenger.datadeling.models.MeldekortPeriodeDTO
import no.nav.dagpenger.datadeling.models.OpprettetAvDTO
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.meldekort.MeldekortregisterClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MeldekortServiceTest {
    private val ident = "01020312345"
    private val meldekortregisterClient =
        MeldekortregisterClient(
            dpMeldekortregisterUrl = "http://localhost:8093",
            tokenProvider = { "token" },
        )
    private val meldekortService = MeldekortService(meldekortregisterClient)

    companion object {
        private lateinit var proxyMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun setupServer() {
            proxyMockServer =
                WireMockServer(8093).also {
                    it.start()
                }
        }

        @AfterAll
        @JvmStatic
        fun tearDownServer() {
            proxyMockServer.shutdownServer()
        }
    }

    @BeforeEach
    fun beforeEach() {
        proxyMockServer.resetAll()
    }

    @Test
    fun `ingen meldekort`() {
        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))

        mockResponse(emptyList())

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(emptyList<MeldekortDTO>(), response)
    }

    @Test
    fun `ett meldekort`() {
        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))

        val meldekort =
            listOf(
                opprettMeldekort(LocalDate.now().minusDays(14)),
            )
        mockResponse(meldekort)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(meldekort, response)
    }

    @Test
    fun `flere meldekort`() {
        val request = enDatadelingRequest(1.januar(2025)..10.januar(2025))

        val meldekort =
            listOf(
                opprettMeldekort(LocalDate.now().minusDays(42)),
                opprettMeldekort(LocalDate.now().minusDays(28)),
                opprettMeldekort(LocalDate.now().minusDays(14)),
            )
        mockResponse(meldekort)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(meldekort, response)
    }

    private fun opprettMeldekort(fraOgMed: LocalDate) =
        MeldekortDTO(
            id = UUID.randomUUID().toString(),
            ident = "01020312345",
            status = MeldekortDTO.Status.Innsendt,
            type = MeldekortDTO.Type.Original,
            periode =
                MeldekortPeriodeDTO(
                    fraOgMed = fraOgMed,
                    tilOgMed = fraOgMed.plusDays(13),
                ),
            dager = emptyList(),
            kanSendes = false,
            kanEndres = true,
            kanSendesFra = fraOgMed.plusDays(11),
            sisteFristForTrekk = fraOgMed.plusDays(20),
            opprettetAv = OpprettetAvDTO.Dagpenger,
            migrert = false,
            originalMeldekortId = null,
            begrunnelse = null,
            kilde =
                MeldekortKildeDTO(
                    rolle = MeldekortKildeDTO.Rolle.Bruker,
                    ident = "01020312345",
                ),
            innsendtTidspunkt = fraOgMed.plusDays(14).toString(),
            registrertArbeidssoker = true,
            meldedato = fraOgMed.plusDays(14),
        )

    private fun mockResponse(
        response: List<MeldekortDTO>,
        delayMs: Int = 0,
    ) {
        proxyMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/datadeling/meldekort"))
                .willReturn(WireMock.jsonResponse(response, 200).withFixedDelay(delayMs)),
        )
    }

    private fun enDatadelingRequest(
        periode: ClosedRange<LocalDate>,
        fnr: String = ident,
    ) = DatadelingRequestDTO(
        fraOgMedDato = periode.start,
        tilOgMedDato = periode.endInclusive,
        personIdent = fnr,
    )
}
