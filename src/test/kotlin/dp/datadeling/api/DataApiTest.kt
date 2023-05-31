package dp.datadeling.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.SignedJWT
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.iverksett.AktivitetType
import no.nav.dagpenger.kontrakter.iverksett.DatoperiodeDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeDagpengerDto
import no.nav.dagpenger.kontrakter.iverksett.VedtaksperiodeType
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.time.LocalDate
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

        val iverksettResponse = VedtaksperiodeDagpengerDto(
            fraOgMedDato = LocalDate.now().minusDays(14),
            tilOgMedDato = LocalDate.now().plusDays(14),
            periode = DatoperiodeDto(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(10)
            ),
            aktivitet = AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
            periodeType = VedtaksperiodeType.HOVEDPERIODE
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
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), VedtaksperiodeDagpengerDto::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(iverksettResponse, apiResponse)
    }

    @Test
    fun shouldGetDataFromArena() = setUpTestApplication {
        // TODO
    }

}