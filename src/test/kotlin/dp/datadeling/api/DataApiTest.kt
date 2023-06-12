package dp.datadeling.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.SignedJWT
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.felles.BrevmottakerDto
import no.nav.dagpenger.kontrakter.felles.Datoperiode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.dagpenger.kontrakter.felles.Tilbakekrevingsvalg
import no.nav.dagpenger.kontrakter.iverksett.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DataApiTest : TestBase() {

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.get("/data/01020312345")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun shouldGet500If500FromIverksett() = setUpTestApplication {
        System.setProperty("IVERKSETT_API_URL", "http://localhost:8092/api")

        val fnr = "01020312345"

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun shouldGet404IfNotFoundInIverksettAndNotFoundInArena() = setUpTestApplication {
        System.setProperty("IVERKSETT_API_URL", "http://localhost:8092/api")

        val fnr = "01020312345"

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun shouldGetDataFromIverksett() = setUpTestApplication {
        System.setProperty("IVERKSETT_API_URL", "http://localhost:8092/api")

        val fnr = "01020312345"

        val iverksettResponse = VedtaksdetaljerDto(
            vedtakstype = VedtakType.RAMMEVEDTAK,
            vedtakstidspunkt = LocalDateTime.now(),
            resultat = Vedtaksresultat.INNVILGET,
            saksbehandlerId = "S12345",
            beslutterId = "B12345",
            opphorAarsak = OpphørÅrsak.PERIODE_UTLØPT,
            avslagAarsak = AvslagÅrsak.MANGLENDE_OPPLYSNINGER,
            utbetalinger = listOf(
                UtbetalingDto(
                    belopPerDag = 100,
                    fraOgMedDato = LocalDate.now().minusDays(7),
                    tilOgMedDato = LocalDate.now().minusDays(14),
                    stonadstype = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    ferietillegg = Ferietillegg.ORDINAER
                )
            ),
            vedtaksperioder = listOf(
                VedtaksperiodeDto(
                    fraOgMedDato = LocalDate.now(),
                    tilOgMedDato = LocalDate.now().plusDays(7),
                    periodeType = VedtaksperiodeType.HOVEDPERIODE
                )
            ),
            tilbakekreving = TilbakekrevingDto(
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                tilbakekrevingMedVarsel = TilbakekrevingMedVarselDto(
                    varseltekst = "Bla bla bla",
                    sumFeilutbetaling = BigDecimal.TEN,
                    fellesperioder = listOf(
                        Datoperiode(
                            fom = LocalDate.now().minusDays(14),
                            tom = LocalDate.now().minusDays(7)
                        )
                    )
                )
            ),
            brevmottakere = listOf(
                BrevmottakerDto(
                    ident = "01020312345",
                    navn = "Test Testesen",
                    mottakerRolle = BrevmottakerDto.MottakerRolle.BRUKER,
                    identType = BrevmottakerDto.IdentType.PERSONIDENT
                )
            )
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(iverksettResponse))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), VedtaksdetaljerDto::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(iverksettResponse, apiResponse)
    }

    @Test
    fun shouldGetDataFromArena() = setUpTestApplication {
        // TODO
    }

}