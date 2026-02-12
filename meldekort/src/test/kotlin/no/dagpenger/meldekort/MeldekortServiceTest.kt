package no.dagpenger.meldekort

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOStatusDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOTypeDTO
import no.nav.dagpenger.datadeling.models.MeldekortKildeDTO
import no.nav.dagpenger.datadeling.models.MeldekortKildeDTORolleDTO
import no.nav.dagpenger.datadeling.models.MeldekortPeriodeDTO
import no.nav.dagpenger.datadeling.models.OpprettetAvDTO
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.meldekort.MeldekortregisterClient
import no.nav.dagpenger.meldekort.MeldepliktAdapterClient
import no.nav.dagpenger.meldekort.Periode
import no.nav.dagpenger.meldekort.Rapporteringsperiode
import no.nav.dagpenger.meldekort.RapporteringsperiodeStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
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
    private val meldepliktAdapterClient =
        MeldepliktAdapterClient(
            dpMeldepliktAdapterUrl = "http://localhost:8093",
            tokenProvider = { "token" },
        )
    private val meldekortService = MeldekortService(meldekortregisterClient, meldepliktAdapterClient)

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
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom, tom)

        mockResponse(emptyList())

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(emptyList(), response)
    }

    @Test
    fun `ett meldekort fra meldekortregister`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom, tom)

        val meldekort =
            listOf(
                opprettMeldekort(LocalDate.now().minusDays(14)),
            )
        mockResponse(meldekort)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(meldekort, response)
    }

    @Test
    fun `flere meldekort fra meldekortregister`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom, tom)

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

    @Test
    fun `ett innsendt meldekort fra arena-meldeplikt-adapter`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom, tom)

        val innsendt =
            listOf(
                opprettRapporteringsperiode(
                    id = 1,
                    status = RapporteringsperiodeStatus.Innsendt,
                    fraOgMed = fom,
                    mottattDato = tom.minusDays(1),
                    registrertArbeidssoker = true,
                ),
            )
        mockResponse(emptyList(), innsendt)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        sjekkInnsendt(innsendt[0], response[0])
    }

    @Test
    fun `ett meldekort til utfylling fra arena-meldeplikt-adapter`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom, tom)

        val tilUtfylling =
            listOf(
                opprettRapporteringsperiode(
                    id = 2,
                    status = RapporteringsperiodeStatus.TilUtfylling,
                    fraOgMed = fom,
                ),
            )
        mockResponse(emptyList(), emptyList(), tilUtfylling)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        sjekkTilUtfylling(tilUtfylling[0], response[0])
    }

    @Test
    fun `meldekort både innsendt og til utfylling fra arena-meldeplikt-adapter`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val request = enDatadelingRequest(fom)

        val innsendt =
            listOf(
                opprettRapporteringsperiode(
                    id = 1,
                    status = RapporteringsperiodeStatus.Innsendt,
                    fraOgMed = fom,
                    mottattDato = tom.minusDays(1),
                    registrertArbeidssoker = true,
                ),
                opprettRapporteringsperiode(
                    id = 3,
                    status = RapporteringsperiodeStatus.Innsendt,
                    fraOgMed = fom,
                    mottattDato = tom.minusDays(1),
                    registrertArbeidssoker = true,
                    begrunnelseEndring = "Begrunnelse",
                ),
            )
        val tilUtfylling =
            listOf(
                opprettRapporteringsperiode(
                    id = 2,
                    status = RapporteringsperiodeStatus.TilUtfylling,
                    fraOgMed = tom,
                ),
            )
        mockResponse(emptyList(), innsendt, tilUtfylling)

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(3, response.size)

        sjekkInnsendt(innsendt[0], response[0])
        sjekkInnsendt(innsendt[1], response[1], "1")
        sjekkTilUtfylling(tilUtfylling[0], response[2])
    }

    @Test
    fun `NoContent fra arena-meldeplikt-adapter gir tom liste`() {
        val fom = LocalDate.now().minusDays(28)
        val request = enDatadelingRequest(fom)

        mockResponse(emptyList())

        val response = runBlocking { meldekortService.hentMeldekort(request) }

        assertEquals(0, response.size)
    }

    @Test
    fun `feil fra arena-meldeplikt-adapter gir exception`() {
        val fom = LocalDate.now().minusDays(42)
        val request = enDatadelingRequest(fom)

        mockResponse(emptyList())

        val exception =
            assertThrows<RuntimeException> {
                runBlocking { meldekortService.hentMeldekort(request) }
            }

        assertEquals("Klarte ikke å hente innsendte meldekort", exception.message)
    }

    private fun opprettMeldekort(fraOgMed: LocalDate) =
        MeldekortDTO(
            id = UUID.randomUUID().toString(),
            ident = "01020312345",
            status = MeldekortDTOStatusDTO.INNSENDT,
            type = MeldekortDTOTypeDTO.ORDINAERT,
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
            opprettetAv = OpprettetAvDTO.DAGPENGER,
            originalMeldekortId = null,
            begrunnelse = null,
            kilde =
                MeldekortKildeDTO(
                    rolle = MeldekortKildeDTORolleDTO.BRUKER,
                    ident = "01020312345",
                ),
            innsendtTidspunkt = fraOgMed.plusDays(14).atStartOfDay(),
            registrertArbeidssoker = true,
            meldedato = fraOgMed.plusDays(14),
        )

    private fun opprettRapporteringsperiode(
        id: Long,
        status: RapporteringsperiodeStatus,
        fraOgMed: LocalDate,
        mottattDato: LocalDate? = null,
        registrertArbeidssoker: Boolean? = null,
        begrunnelseEndring: String? = null,
    ) = Rapporteringsperiode(
        id = id,
        periode =
            Periode(
                fraOgMed = fraOgMed,
                tilOgMed = fraOgMed.plusDays(13),
            ),
        dager = emptyList(),
        kanSendesFra = fraOgMed.plusDays(11),
        kanSendes = false,
        kanEndres = true,
        status = status,
        mottattDato = mottattDato,
        bruttoBelop = 0.0,
        registrertArbeidssoker = registrertArbeidssoker,
        begrunnelseEndring = begrunnelseEndring,
    )

    private fun sjekkInnsendt(
        expected: Rapporteringsperiode,
        actual: MeldekortDTO,
        originalMeldekortId: String? = null,
    ) {
        val type = if (originalMeldekortId == null) MeldekortDTOTypeDTO.ORDINAERT else MeldekortDTOTypeDTO.KORRIGERT

        assertEquals(expected.id.toString(), actual.id)
        assertEquals(ident, actual.ident)
        assertEquals(MeldekortDTOStatusDTO.INNSENDT, actual.status)
        assertEquals(type, actual.type)
        assertEquals(expected.periode.fraOgMed, actual.periode.fraOgMed)
        assertEquals(expected.periode.tilOgMed, actual.periode.tilOgMed)
        assertEquals(expected.kanSendes, actual.kanSendes)
        assertEquals(expected.kanEndres, actual.kanEndres)
        assertEquals(expected.kanSendesFra, actual.kanSendesFra)
        assertEquals(expected.periode.tilOgMed.plusDays(8), actual.sisteFristForTrekk)
        assertEquals(OpprettetAvDTO.ARENA, actual.opprettetAv)
        assertEquals(originalMeldekortId, actual.originalMeldekortId)
        assertEquals(expected.begrunnelseEndring, actual.begrunnelse)
        assertEquals(MeldekortKildeDTORolleDTO.BRUKER, actual.kilde?.rolle)
        assertEquals(ident, actual.kilde?.ident)
        assertEquals(expected.mottattDato, actual.innsendtTidspunkt?.toLocalDate())
        assertEquals(expected.registrertArbeidssoker, actual.registrertArbeidssoker)
        assertEquals(expected.mottattDato, actual.meldedato)
    }

    private fun sjekkTilUtfylling(
        expected: Rapporteringsperiode,
        actual: MeldekortDTO,
    ) {
        assertEquals(expected.id.toString(), actual.id)
        assertEquals(ident, actual.ident)
        assertEquals(MeldekortDTOStatusDTO.TIL_UTFYLLING, actual.status)
        assertEquals(MeldekortDTOTypeDTO.ORDINAERT, actual.type)
        assertEquals(expected.periode.fraOgMed, actual.periode.fraOgMed)
        assertEquals(expected.periode.tilOgMed, actual.periode.tilOgMed)
        assertEquals(expected.kanSendes, actual.kanSendes)
        assertEquals(expected.kanEndres, actual.kanEndres)
        assertEquals(expected.kanSendesFra, actual.kanSendesFra)
        assertEquals(expected.periode.tilOgMed.plusDays(8), actual.sisteFristForTrekk)
        assertEquals(OpprettetAvDTO.ARENA, actual.opprettetAv)
        assertEquals(null, actual.originalMeldekortId)
        assertEquals(null, actual.begrunnelse)
        assertEquals(null, actual.kilde)
        assertEquals(null, actual.innsendtTidspunkt)
        assertEquals(null, actual.registrertArbeidssoker)
        assertEquals(null, actual.meldedato)
    }

    private fun mockResponse(
        meldekortregisterResponse: List<MeldekortDTO>,
        meldepliktAdapterInnsendteResponse: List<Rapporteringsperiode> = emptyList(),
        meldepliktAdapterTilUtfyllingResponse: List<Rapporteringsperiode> = emptyList(),
        delayMs: Int = 0,
    ) {
        proxyMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/datadeling/meldekort"))
                .willReturn(
                    WireMock
                        .jsonResponse(
                            jacksonObjectMapper()
                                .apply {
                                    registerModule(JavaTimeModule())
                                }.writeValueAsString(meldekortregisterResponse),
                            200,
                        ).withFixedDelay(delayMs),
                ),
        )
        proxyMockServer.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/sendterapporteringsperioder?antallMeldeperioder=1"))
                .willReturn(
                    WireMock
                        .jsonResponse(
                            jacksonObjectMapper()
                                .apply {
                                    registerModule(JavaTimeModule())
                                }.writeValueAsString(meldepliktAdapterInnsendteResponse),
                            200,
                        ).withFixedDelay(delayMs),
                ),
        )
        proxyMockServer.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/sendterapporteringsperioder?antallMeldeperioder=2"))
                .willReturn(
                    WireMock
                        .jsonResponse(
                            "",
                            204,
                        ).withFixedDelay(delayMs),
                ),
        )
        proxyMockServer.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/rapporteringsperioder"))
                .willReturn(
                    WireMock
                        .jsonResponse(
                            jacksonObjectMapper()
                                .apply {
                                    registerModule(JavaTimeModule())
                                }.writeValueAsString(meldepliktAdapterTilUtfyllingResponse),
                            200,
                        ).withFixedDelay(delayMs),
                ),
        )
    }

    private fun enDatadelingRequest(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate? = null,
        fnr: String = ident,
    ) = DatadelingRequestDTO(
        fraOgMedDato = fraOgMed,
        tilOgMedDato = tilOgMed,
        personIdent = fnr,
    )
}
