package dp.datadeling.api

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

class DataV1ApiTest : TestBase() {

    private val fnr = "01020312342"
    private val datadelingRequest = DatadelingRequest(
        personIdent = fnr,
        fraOgMedDato = LocalDate.now().minusDays(20),
        tilOgMedDato = null
    )
    private val emptyResponse = DatadelingResponse(
        personIdent = fnr,
        perioder = emptyList()
    )
    private val dpIverksettPerioder = listOf(
        Periode(
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now().plusDays(14),
            ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
            gjenståendeDager = 123
        )
    )
    private val dpIverksettResponse = DatadelingResponse(
        personIdent = fnr,
        perioder = dpIverksettPerioder
    )

    private val dpProxyPerioder = listOf(
        Periode(
            fraOgMedDato = LocalDate.now().minusDays(15),
            tilOgMedDato = LocalDate.now().minusDays(1),
            ytelseType = StønadType.DAGPENGER_PERMITTERING_ORDINAER,
            gjenståendeDager = 0
        )
    )
    private val dpProxyResponse = DatadelingResponse(
        personIdent = fnr,
        perioder = dpProxyPerioder
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
        setUpMock(null, emptyResponse)
        val token = createToken()

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
        setUpMock(emptyResponse, null)
        val token = createToken()

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
        setUpMock(dpIverksettResponse, emptyResponse)
        val token = createToken()

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
        assertEquals(dpIverksettPerioder, apiResponse.perioder)
    }

    @Test
    fun shouldGetDataFromDpProxy() = setUpTestApplication {
        setUpMock(emptyResponse, dpProxyResponse)
        val token = createToken()

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
        assertEquals(dpProxyPerioder, apiResponse.perioder)
    }

    @Test
    fun shouldGetDataFromDpIverksettAndDpProxy() = setUpTestApplication {
        setUpMock(dpIverksettResponse, dpProxyResponse)
        val token = createToken()

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

    @Test
    fun shouldRewriteFomDate() = setUpTestApplication {
        setUpMock(dpIverksettResponse, dpProxyResponse)
        val token = createToken()

        val fom = LocalDate.now().minusDays(5)
        val datadelingRequest = DatadelingRequest(
            personIdent = fnr,
            fraOgMedDato = fom,
            tilOgMedDato = null
        )

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
        // FOM-dato i denne perioden skal erstattes med FOM fra request'en
        assertEquals(fom, apiResponse.perioder[0].fraOgMedDato)
        assertEquals(dpProxyPerioder[0].tilOgMedDato, apiResponse.perioder[0].tilOgMedDato)
        assertEquals(dpIverksettPerioder[0].fraOgMedDato, apiResponse.perioder[1].fraOgMedDato)
        assertEquals(dpIverksettPerioder[0].tilOgMedDato, apiResponse.perioder[1].tilOgMedDato)
    }

    @Test
    fun shouldRewriteToDate() = setUpTestApplication {
        setUpMock(dpIverksettResponse, dpProxyResponse)
        val token = createToken()

        val tom = LocalDate.now().plusDays(5)
        val datadelingRequest = DatadelingRequest(
            personIdent = fnr,
            fraOgMedDato = LocalDate.now().minusDays(20),
            tilOgMedDato = tom
        )

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
        assertEquals(dpProxyPerioder[0].fraOgMedDato, apiResponse.perioder[0].fraOgMedDato)
        assertEquals(dpProxyPerioder[0].tilOgMedDato, apiResponse.perioder[0].tilOgMedDato)
        assertEquals(dpIverksettPerioder[0].fraOgMedDato, apiResponse.perioder[1].fraOgMedDato)
        // TOM-dato i denne perioden skal erstattes med TOM fra request'en
        assertEquals(tom, apiResponse.perioder[1].tilOgMedDato)
    }

    private fun createToken(): SignedJWT {
        return mockOAuth2Server.issueToken(ISSUER_ID, "someclientid", DefaultOAuth2TokenCallback())
    }
}
