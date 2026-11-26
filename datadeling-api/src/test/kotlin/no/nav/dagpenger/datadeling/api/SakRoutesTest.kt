package no.nav.dagpenger.datadeling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.BehandlingResultat
import no.nav.dagpenger.behandling.BehandlingResultatRepositoryMedTolker
import no.nav.dagpenger.behandling.BeregnetDag
import no.nav.dagpenger.behandling.Periodisert
import no.nav.dagpenger.behandling.Rettighetsperiode
import no.nav.dagpenger.behandling.Rettighetstyper
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.SakBehandling
import no.nav.dagpenger.datadeling.api.TestApplication.issueAzureToken
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.config.Tilgangsrolle
import no.nav.dagpenger.datadeling.models.DagDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOStatusDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOTypeDTO
import no.nav.dagpenger.datadeling.models.MeldekortPeriodeDTO
import no.nav.dagpenger.datadeling.models.OpprettetAvDTO
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testGet
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.meldekort.MeldekortService
import no.nav.dagpenger.søknad.SøknadBehandlingGraf
import no.nav.dagpenger.søknad.modell.SøknadMedBehandlinger
import no.nav.dagpenger.søknad.modell.Søknadsbehandling
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import org.intellij.lang.annotations.Language
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class SakRoutesTest {
    private val idag = LocalDate.now()
    private val sakId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val annenSakId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val opprettet = LocalDateTime.of(2024, 3, 1, 12, 0, 0)

    private val repository = mockk<BehandlingResultatRepositoryMedTolker>()
    private val meldekortService = mockk<MeldekortService>()
    private val graf = FakeSøknadBehandlingGraf()
    private val sakService = SakService(repository, meldekortService, graf)
    private val søknadId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val innsendt = LocalDateTime.of(2024, 2, 1, 9, 30, 0)

    private fun enSøknadsbehandling(
        sakId: UUID,
        tilstand: SøknadsbehandlingTilstand = SøknadsbehandlingTilstand.UnderBehandling,
        søknadId: UUID = this.søknadId,
        innsendt: LocalDateTime = this.innsendt,
        utfall: String? = null,
    ) = Søknadsbehandling(søknadId, sakId, innsendt, tilstand, utfall)

    private val token get() = issueAzureToken(azpRoles = listOf(Tilgangsrolle.nks.name))

    private val sakerUrl = "/dagpenger/datadeling/v1/saker"

    @Test
    fun `returnerer 401 uten token`() =
        testEndepunkter(sakService = sakService) {
            client.testPost(sakerUrl, enDatadelingRequest(), token = null).status shouldBe HttpStatusCode.Unauthorized
            client.testGet("$sakerUrl/$sakId", token = null).status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `returnerer 403 uten riktig rolle`() =
        testEndepunkter(sakService = sakService) {
            client.testPost(sakerUrl, enDatadelingRequest(), issueAzureToken(azpRoles = listOf("ROLE"))).status shouldBe
                HttpStatusCode.Forbidden
        }

    @Test
    fun `returnerer tom liste når personen ikke har saker`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()

            val response = client.testPost(sakerUrl, enDatadelingRequest(), token)

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldEqualJson "[]"
        }

    @Test
    fun `returnerer tynne saker uten meldekort og beregninger som standard`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns listOf(enLøpendeSak())

            val response = client.testPost(sakerUrl, enDatadelingRequest(), token)

            response.status shouldBe HttpStatusCode.OK
            @Language("JSON")
            response.bodyAsText() shouldEqualJson
                """
                [
                  {
                    "status": "Løpende",
                    "sakId": "$sakId",
                    "rettighetsperioder": [
                      { "fraOgMed": "${idag.minusDays(30)}", "harRett": true }
                    ],
                    "gjenstående": { "dager": 258 },
                    "sistEndret": "2024-03-01T12:00:00"
                  }
                ]
                """.trimIndent()
        }

    @Test
    fun `include=opplysninger gir perioder, og med dato kun verdien som gjelder`() =
        testEndepunkter(sakService = sakService) {
            val sak = enLøpendeSak()
            every { repository.hentSaker(any()) } returns listOf(sak)

            val perioder =
                objectMapper.readTree(
                    client.testPost("$sakerUrl?include=opplysninger", enDatadelingRequest(), token).bodyAsText(),
                )[0]
            perioder["opplysninger"]["visning"].asText() shouldBe "Perioder"
            perioder["opplysninger"]["antallBarn"].elementer().size shouldBe 2

            val dato = idag.minusDays(1)
            val enkeltdag =
                objectMapper.readTree(
                    client.testPost("$sakerUrl?include=opplysninger&gjelderFor=$dato", enDatadelingRequest(), token).bodyAsText(),
                )[0]
            enkeltdag["opplysninger"]["visning"].asText() shouldBe "Enkeltdag"
            enkeltdag["opplysninger"]["gjelderFor"].asText() shouldBe dato.toString()
            enkeltdag["opplysninger"]["antallBarn"].asInt() shouldBe 2

            val utenInclude = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]
            utenInclude.has("opplysninger") shouldBe false
        }

    @Test
    fun `ugyldig dato gir 400`() =
        testEndepunkter(sakService = sakService) {
            client.testPost("$sakerUrl?include=opplysninger&gjelderFor=i-går", enDatadelingRequest(), token).status shouldBe
                HttpStatusCode.BadRequest
        }

    @Test
    fun `include=meldekort tar med meldekort men ikke beregninger`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns listOf(enLøpendeSak())
            coEvery { meldekortService.hentMeldekort(any()) } returns listOf(etMeldekort("mk-1"))

            val body = client.testPost("$sakerUrl?include=meldekort", enDatadelingRequest(), token).bodyAsText()

            val sak = objectMapper.readTree(body)[0]
            sak["meldekort"].elementer().map { it["id"].asText() } shouldBe listOf("mk-1")
            sak.has("beregninger") shouldBe false
        }

    @Test
    fun `include=beregninger tar med beregninger men ikke meldekort`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns listOf(enLøpendeSak())

            val body = client.testPost("$sakerUrl?include=beregninger", enDatadelingRequest(), token).bodyAsText()

            val sak = objectMapper.readTree(body)[0]
            sak.has("meldekort") shouldBe false
            sak["beregninger"].elementer().map { it["fraOgMed"].asText() } shouldBe
                listOf("${idag.minusDays(14)}", "${idag.minusDays(7)}")
            sak["beregninger"][0]["kilde"].asText() shouldBe "DP_SAK"
            sak["beregninger"][1]["gjenståendeDager"].asInt() shouldBe 258
        }

    @Test
    fun `include=meldekort,beregninger tar med begge`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns listOf(enLøpendeSak())
            coEvery { meldekortService.hentMeldekort(any()) } returns listOf(etMeldekort("mk-1"))

            val body = client.testPost("$sakerUrl?include=meldekort,beregninger", enDatadelingRequest(), token).bodyAsText()

            val sak = objectMapper.readTree(body)[0]
            sak["meldekort"].size() shouldBe 1
            sak["beregninger"].size() shouldBe 2
        }

    @Test
    fun `sak med resultat uten periode og uten søknader er TilBehandling uten soknader`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns
                listOf(Sak(sakId, listOf(SakBehandling(opprettet, behandling(perioder = emptyList())))))

            client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText() shouldEqualJson
                """[{ "status": "TilBehandling", "sakId": "$sakId" }]"""
        }

    @Test
    fun `sak med resultat uten periode og med avbrutt søknad er TilBehandling med søknaden`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns
                listOf(Sak(sakId, listOf(SakBehandling(opprettet, behandling(perioder = emptyList())))))
            graf.søknadsbehandlinger =
                listOf(enSøknadsbehandling(sakId, SøknadsbehandlingTilstand.Avbrutt))

            val sak = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]
            sak["status"].asString() shouldBe "TilBehandling"
            sak["soknader"][0]["tilstand"].asString() shouldBe "Avbrutt"
        }

    @Test
    fun `sak med resultat uten periode men med søknad under behandling er TilBehandling med tomme meldekort`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns
                listOf(Sak(sakId, listOf(SakBehandling(opprettet, behandling(perioder = emptyList())))))
            graf.søknadsbehandlinger = listOf(enSøknadsbehandling(sakId))
            coEvery { meldekortService.hentMeldekort(any()) } returns emptyList()

            @Language("JSON")
            client.testPost("$sakerUrl?include=meldekort,beregninger", enDatadelingRequest(), token).bodyAsText() shouldEqualJson
                """
                [
                  {
                    "status": "TilBehandling", "sakId": "$sakId", "meldekort": [],
                    "soknader": [
                      { "soknadId": "$søknadId", "innsendt": "2024-02-01T09:30:00", "tilstand": "UnderBehandling" }
                    ]
                  }
                ]
                """.trimIndent()
        }

    @Test
    fun `løpende sak får alle søknader, nyeste først, med utfall bare når ferdig`() =
        testEndepunkter(sakService = sakService) {
            val eldre = UUID.fromString("44444444-4444-4444-4444-444444444444")
            every { repository.hentSaker(any()) } returns listOf(enLøpendeSak())
            graf.søknadsbehandlinger =
                listOf(
                    enSøknadsbehandling(sakId, SøknadsbehandlingTilstand.Ferdig, eldre, innsendt.minusDays(10), "Innvilgelse"),
                    enSøknadsbehandling(sakId),
                )

            val sak = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]
            sak["status"].asString() shouldBe "Løpende"
            sak["soknader"].elementer().map { it["soknadId"].asString() } shouldBe listOf("$søknadId", "$eldre")
            sak["soknader"][0]["tilstand"].asString() shouldBe "UnderBehandling"
            sak["soknader"][0].has("utfall") shouldBe false
            sak["soknader"][1]["utfall"].asString() shouldBe "Innvilgelse"
        }

    @Test
    fun `pause i løpende sak er Løpende med siste rettighetsperiode uten rett`() =
        testEndepunkter(sakService = sakService) {
            val pause =
                Sak(
                    sakId,
                    listOf(
                        SakBehandling(
                            opprettet,
                            behandling(
                                perioder =
                                    listOf(
                                        periode(idag.minusDays(60), idag.minusDays(11)),
                                        periode(idag.minusDays(10), null, harRett = false),
                                    ),
                            ),
                        ),
                    ),
                )
            every { repository.hentSaker(any()) } returns listOf(pause)

            val sak = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]

            sak["status"].asText() shouldBe "Løpende"
            sak["rettighetsperioder"].last()["harRett"].asBoolean() shouldBe false
            sak["rettighetsperioder"].size() shouldBe 2
        }

    @Test
    fun `avsluttet sak serialiseres med tilOgMed og uten gjenstående dager`() =
        testEndepunkter(sakService = sakService) {
            val fra = idag.minusDays(100)
            val til = idag.minusDays(10)
            every { repository.hentSaker(any()) } returns
                listOf(Sak(sakId, listOf(SakBehandling(opprettet, behandling(perioder = listOf(periode(fra, til)))))))

            val response = client.testPost("$sakerUrl?include=beregninger", enDatadelingRequest(), token)

            response.status shouldBe HttpStatusCode.OK
            @Language("JSON")
            response.bodyAsText() shouldEqualJson
                """
                [
                  {
                    "status": "Avsluttet",
                    "sakId": "$sakId",
                    "rettighetsperioder": [
                      { "fraOgMed": "$fra", "tilOgMed": "$til", "harRett": true }
                    ],
                    "sistEndret": "2024-03-01T12:00:00",
                    "beregninger": []
                  }
                ]
                """.trimIndent()
        }

    @Test
    fun `returnerer flere saker med ulik status`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns
                listOf(
                    enLøpendeSak(),
                    Sak(
                        annenSakId,
                        listOf(SakBehandling(opprettet, behandling(perioder = listOf(periode(idag.minusDays(100), idag.minusDays(10)))))),
                    ),
                )

            val saker = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())

            saker.elementer().map { it["sakId"].asText() to it["status"].asText() } shouldBe
                listOf("$sakId" to "Løpende", "$annenSakId" to "Avsluttet")
        }

    @Test
    fun `henter enkelt sak med meldekort og beregninger`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSak(sakId) } returns enLøpendeSak()
            coEvery { meldekortService.hentMeldekort(any()) } returns listOf(etMeldekort("mk-1"))

            val response = client.testGet("$sakerUrl/$sakId?include=meldekort,beregninger", token)

            response.status shouldBe HttpStatusCode.OK
            val sak = objectMapper.readTree(response.bodyAsText())
            sak["status"].asText() shouldBe "Løpende"
            sak["sakId"].asText() shouldBe "$sakId"
            sak["meldekort"].size() shouldBe 1
            sak["beregninger"].size() shouldBe 2
        }

    @Test
    fun `henter enkelt sak uten include som tynn sak`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSak(sakId) } returns enLøpendeSak()

            val sak = objectMapper.readTree(client.testGet("$sakerUrl/$sakId", token).bodyAsText())

            sak.has("meldekort") shouldBe false
            sak.has("beregninger") shouldBe false
        }

    @Test
    fun `returnerer 404 for ukjent sakId`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSak(any()) } returns null

            client.testGet("$sakerUrl/${UUID.randomUUID()}", token).status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `søknad med sak men uten resultat er TilBehandling i liste og ved oppslag`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()
            every { repository.hentSak(annenSakId) } returns null
            graf.søknadsbehandlinger = listOf(enSøknadsbehandling(annenSakId))

            val forventet =
                """{ "status": "TilBehandling", "sakId": "$annenSakId", "soknader": [{ "soknadId": "$søknadId", "innsendt": "2024-02-01T09:30:00", "tilstand": "UnderBehandling" }] }"""
            client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText() shouldEqualJson "[$forventet]"
            client.testGet("$sakerUrl/$annenSakId", token).bodyAsText() shouldEqualJson forventet
        }

    @Test
    fun `kun avbrutt søknad uten resultat er likevel en sak med status TilBehandling`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()
            every { repository.hentSak(annenSakId) } returns null
            val avbrutt = listOf(enSøknadsbehandling(annenSakId, SøknadsbehandlingTilstand.Avbrutt))
            graf.søknadsbehandlinger = avbrutt

            val sak = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]
            sak["sakId"].asString() shouldBe "$annenSakId"
            sak["status"].asString() shouldBe "TilBehandling"
            sak["soknader"][0]["tilstand"].asString() shouldBe "Avbrutt"
            client.testGet("$sakerUrl/$annenSakId", token).status shouldBe HttpStatusCode.OK
        }

    @Test
    fun `innsendt søknad uten sak vises ikke i listen, mens Innsendt med sak mappes`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()
            val utenSak = enSøknadsbehandling(sakId, SøknadsbehandlingTilstand.Innsendt, UUID.randomUUID()).copy(sakId = null)
            graf.søknadsbehandlinger =
                listOf(utenSak, enSøknadsbehandling(annenSakId, SøknadsbehandlingTilstand.Innsendt))

            val saker = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())

            saker.size() shouldBe 1
            saker[0]["sakId"].asString() shouldBe "$annenSakId"
            saker[0]["soknader"][0]["tilstand"].asString() shouldBe "Innsendt"
        }

    @Test
    fun `søknad med flere behandlinger i samme sak vises én gang med siste behandlings tilstand`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()
            graf.søknadsbehandlinger =
                listOf(
                    enSøknadsbehandling(annenSakId, SøknadsbehandlingTilstand.Avbrutt).copy(sistEndret = opprettet.minusDays(2)),
                    enSøknadsbehandling(annenSakId, SøknadsbehandlingTilstand.UnderBehandling).copy(sistEndret = opprettet),
                )

            val sak = objectMapper.readTree(client.testPost(sakerUrl, enDatadelingRequest(), token).bodyAsText())[0]

            sak["soknader"].size() shouldBe 1
            sak["soknader"][0]["tilstand"].asString() shouldBe "UnderBehandling"
        }

    @Test
    fun `returnerer 400 for ugyldig include-verdi`() =
        testEndepunkter(sakService = sakService) {
            every { repository.hentSaker(any()) } returns emptyList()

            client.testPost("$sakerUrl?include=meldekort,ugyldig", enDatadelingRequest(), token).status shouldBe
                HttpStatusCode.BadRequest
            client.testGet("$sakerUrl/$sakId?include=ugyldig", token).status shouldBe HttpStatusCode.BadRequest
        }

    private fun JsonNode.elementer() = (0 until size()).map { this[it] }

    private fun enLøpendeSak() =
        Sak(
            sakId,
            listOf(
                SakBehandling(
                    opprettet,
                    behandling(
                        perioder = listOf(periode(idag.minusDays(30), null)),
                        beregninger =
                            listOf(
                                beregnetDag(idag.minusDays(7), gjenståendeDager = 258),
                                beregnetDag(idag.minusDays(14), gjenståendeDager = 259),
                            ),
                    ),
                ),
            ),
        )

    private fun behandling(
        perioder: List<Rettighetsperiode>,
        beregninger: List<BeregnetDag> = emptyList(),
    ) = object : BehandlingResultat {
        override val ident = "01020312342"
        override val behandlingId: UUID = UUID.randomUUID()
        override val rettighetsperioder = perioder
        override val rettighetstyper = emptyList<Rettighetstyper>()
        override val beregninger = beregninger
        override val antallBarn =
            listOf(
                Periodisert<Int>(idag.minusDays(30), idag.minusDays(2), 1),
                Periodisert<Int>(idag.minusDays(1), null, 2),
            )
    }

    private fun periode(
        fra: LocalDate,
        til: LocalDate?,
        harRett: Boolean = true,
    ) = object : Rettighetsperiode {
        override val fraOgMed = fra
        override val tilOgMed = til
        override val harRett = harRett
    }

    private fun beregnetDag(
        dato: LocalDate,
        gjenståendeDager: Int,
    ) = object : BeregnetDag {
        override val dato = dato
        override val sats = 800
        override val utbetaling = 1000
        override val gjenståendeDager = gjenståendeDager
    }

    private fun etMeldekort(id: String) =
        MeldekortDTO(
            id = id,
            ident = "01020312342",
            status = MeldekortDTOStatusDTO.INNSENDT,
            type = MeldekortDTOTypeDTO.ORDINAERT,
            periode = MeldekortPeriodeDTO(idag.minusDays(14), idag.minusDays(1)),
            dager = emptyList<DagDTO>(),
            kanSendes = false,
            kanEndres = false,
            kanSendesFra = idag.minusDays(1),
            sisteFristForTrekk = idag.plusDays(5),
            opprettetAv = OpprettetAvDTO.DAGPENGER,
        )
}

private class FakeSøknadBehandlingGraf : SøknadBehandlingGraf {
    var søknadsbehandlinger: List<Søknadsbehandling> = emptyList()

    private fun List<Søknadsbehandling>.gruppert() = groupBy { it.søknadId }.map { (id, liste) -> SøknadMedBehandlinger(id, liste) }

    override fun søknaderForPerson(ident: String) = søknadsbehandlinger.gruppert()

    override fun søknaderForSak(sakId: UUID) = søknadsbehandlinger.filter { it.sakId == sakId }.gruppert()

    override fun søknadForBehandling(behandlingId: UUID) =
        søknadsbehandlinger
            .filter {
                it.behandlingId == behandlingId
            }.gruppert()
            .singleOrNull()

    override fun resultaterForSøknad(søknadId: UUID) = emptyList<no.nav.dagpenger.behandling.SakBehandling>()
}
