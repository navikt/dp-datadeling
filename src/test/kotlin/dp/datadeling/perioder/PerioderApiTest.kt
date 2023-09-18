package dp.datadeling.perioder

import com.nimbusds.jwt.SignedJWT
import dp.datadeling.api.ApiTestBase
import dp.datadeling.enDatadelingRequest
import dp.datadeling.enDatadelingResponse
import dp.datadeling.enPeriode
import dp.datadeling.januar
import dp.datadeling.utils.defaultObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class PerioderApiTest : ApiTestBase() {

    private val perioderService: PerioderService = mockk()

    @Test
    fun `returnerer 401 uten token`() = testPerioderEndpoint {
        client.post(enDatadelingRequest(), null).apply {
            assertEquals(HttpStatusCode.Unauthorized, this.status)
        }
    }

    @Test
    fun `returnerer 500 hvis henting av data feiler`() = testPerioderEndpoint {
        coEvery { perioderService.hentDagpengeperioder(any()) }.throws(Exception())

        client.post(enDatadelingRequest()).apply {
            assertEquals(HttpStatusCode.InternalServerError, this.status)
        }
    }

    @Test
    fun `returnerer 200 og perioder`() = testPerioderEndpoint {
        val response = enDatadelingResponse(enPeriode(1.januar()..15.januar()))

        coEvery { perioderService.hentDagpengeperioder(any()) } returns response

        client.post(enDatadelingRequest())
            .apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let { defaultObjectMapper.readValue(it.bodyAsText(), DatadelingResponse::class.java) }
            .apply {
                assertEquals(1, this.perioder.size)
                assertEquals(response.personIdent, this.personIdent)
                assertEquals(response.perioder, this.perioder)
            }
    }

    private suspend fun HttpClient.post(
        request: DatadelingRequest,
        token: SignedJWT? = createToken()
    ) =
        post(perioderApiPath) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                if (token != null) {
                    append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
                }
            }
            setBody(defaultObjectMapper.writeValueAsString(request))
        }

    private fun testPerioderEndpoint(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            module {
                perioderApi(perioderService)
            }
            block()
        }
    }
}