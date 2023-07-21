package dp.datadeling.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.SignedJWT
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.iverksett.*
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DataV10ApiTest : TestBase() {

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.get("/data/v1.0/01020312345")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun shouldGet500If500FromIverksett() = setUpTestApplication {
        val fnr = "01020312341"

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/v1.0/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun shouldGet500If500FromDpProxy() = setUpTestApplication {
        val fnr = "01020312342"

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                )
        )
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/v1.0/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun shouldGet404IfNotFoundInIverksettAndNotFoundInArena() = setUpTestApplication {
        val fnr = "01020312343"

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                )
        )
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/v1.0/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun shouldGetDataFromIverksett() = setUpTestApplication {
        val fnr = "01020312344"

        val iverksettResponse = VedtaksstatusDto(
            vedtakstype = VedtakType.RAMMEVEDTAK,
            vedtakstidspunkt = LocalDateTime.now(),
            resultat = Vedtaksresultat.INNVILGET,
            vedtaksperioder = listOf(
                VedtaksperiodeDto(
                    fraOgMedDato = LocalDate.now(),
                    tilOgMedDato = LocalDate.now().plusDays(7),
                    periodeType = VedtaksperiodeType.HOVEDPERIODE
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
        val response = client.get("/data/v1.0/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), VedtaksstatusDto::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(iverksettResponse, apiResponse)
    }

    @Test
    fun shouldGetDataFromArena() = setUpTestApplication {
        val fnr = "01020312343"
        val vedtaksdato = LocalDate.now()

        val dpProxyResponse = listOf(
            DpProxyResponseDto(
                vedtaktypekode = "O",
                vedtakstatuskode = "IVERK",
                fraDato = vedtaksdato.plusDays(1),
                tilDato = vedtaksdato.plusDays(15),
                vedtaksdato = vedtaksdato
            ),
            DpProxyResponseDto(
                vedtaktypekode = "O",
                vedtakstatuskode = "GODKJ",
                fraDato = LocalDate.now(),
                tilDato = LocalDate.now(),
                vedtaksdato = LocalDate.now().minusDays(1)
            ),
            DpProxyResponseDto(
                vedtaktypekode = "O",
                vedtakstatuskode = "AVSLU",
                fraDato = LocalDate.now(),
                tilDato = LocalDate.now(),
                vedtaksdato = LocalDate.now()
            )
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/vedtakstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                )
        )
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus/$fnr"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(dpProxyResponse))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.get("/data/v1.0/$fnr") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), VedtaksstatusDto::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(VedtakType.RAMMEVEDTAK, apiResponse.vedtakstype)
        assertEquals(vedtaksdato.atStartOfDay(), apiResponse.vedtakstidspunkt)
        assertEquals(Vedtaksresultat.INNVILGET, apiResponse.resultat)
        assertEquals(1, apiResponse.vedtaksperioder.size)
        assertEquals(vedtaksdato.plusDays(1), apiResponse.vedtaksperioder[0].fraOgMedDato)
        assertEquals(vedtaksdato.plusDays(15), apiResponse.vedtaksperioder[0].tilOgMedDato)
        assertEquals(VedtaksperiodeType.HOVEDPERIODE, apiResponse.vedtaksperioder[0].periodeType)
    }

}
