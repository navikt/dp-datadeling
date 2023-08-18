package dp.datadeling.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.SignedJWT
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DataV10ApiTest : TestBase() {

    private val fnr = "01020312342"
    private val datadelingRequest = DatadelingRequest(
        personIdent = fnr,
        fraOgMedDato = LocalDate.now(),
        tilOgMedDato = null
    )
    private val datadelingResponse = DatadelingResponse(
        personIdent = fnr,
        perioder = emptyList()
    )

    @Test
    fun shouldGet401WithoutToken() = setUpTestApplication {
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.ContentType, "application/json")
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun shouldGet500If500FromDpIverksett() = setUpTestApplication {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/vedtakstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(datadelingResponse))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(datadelingRequest))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun shouldGet500If500FromDpProxy() = setUpTestApplication {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/vedtakstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(datadelingResponse))
                )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(datadelingRequest))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun shouldGetDataFromDpIverksett() = setUpTestApplication {
        val perioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now().plusDays(14),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )
        val responseMedPerioder = DatadelingResponse(
            personIdent = fnr,
            perioder = perioder
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/vedtakstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(responseMedPerioder))
                )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(datadelingResponse))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(datadelingRequest))
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), DatadelingResponse::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(fnr, apiResponse.personIdent)
        assertEquals(1, apiResponse.perioder.size)
        assertEquals(perioder, apiResponse.perioder)
    }

    @Test
    fun shouldGetDataFromDpProxy() = setUpTestApplication {
        val perioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now().plusDays(14),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )
        val responseMedPerioder = DatadelingResponse(
            personIdent = fnr,
            perioder = perioder
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/vedtakstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(datadelingResponse))
                )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(responseMedPerioder))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(datadelingRequest))
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), DatadelingResponse::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(fnr, apiResponse.personIdent)
        assertEquals(1, apiResponse.perioder.size)
        assertEquals(perioder, apiResponse.perioder)
    }

    @Test
    fun shouldGetDataFromDpIverksettAndDpProxy() = setUpTestApplication {
        val dpIverksettPerioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now().plusDays(14),
                ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                gjenståendeDager = 123
            )
        )
        val dpIverksettResponse = DatadelingResponse(
            personIdent = fnr,
            perioder = dpIverksettPerioder
        )

        val dpProxyPerioder = listOf(
            Periode(
                fraOgMedDato = LocalDate.now().minusDays(15),
                tilOgMedDato = LocalDate.now().minusDays(1),
                ytelseType = StønadType.DAGPENGER_PERMITTERING_ORDINAER,
                gjenståendeDager = 0
            )
        )
        val dpProxyResponse = DatadelingResponse(
            personIdent = fnr,
            perioder = dpProxyPerioder
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/vedtakstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(dpIverksettResponse))
                )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dp-proxy/proxy/v1/arena/vedtaksstatus"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatusCode.OK.value)
                        .withHeader(HttpHeaders.ContentType, "application/json")
                        .withBody(defaultObjectMapper.writeValueAsString(dpProxyResponse))
                )
        )

        val token: SignedJWT = mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
        val response = client.post("/data/v1.0") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(defaultObjectMapper.writeValueAsString(datadelingRequest))
        }
        val apiResponse = defaultObjectMapper.readValue(response.bodyAsText(), DatadelingResponse::class.java)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(fnr, apiResponse.personIdent)
        assertEquals(2, apiResponse.perioder.size)
        assertEquals(dpIverksettPerioder + dpProxyPerioder, apiResponse.perioder)
    }
}
