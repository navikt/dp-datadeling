package no.nav.dagpenger.datadeling.api.perioder.ressurs

import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.datadeling.TestApplication
import no.nav.dagpenger.datadeling.api.datadelingApi
import no.nav.dagpenger.datadeling.testPost
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.mockConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PerioderApiTest2 {

    @Test
    fun `returnerer 401 uten token`() = TestApplication.withMockAuthServerAndTestApplication(
        moduleFunction = {
            datadelingApi(mockConfig())
        },
    ) {
        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), token = null).apply {
            assertEquals(HttpStatusCode.Unauthorized, this.status)
        }
    }
    /*

    @Test
    fun `returnerer 500 hvis oppretting av ressurs feiler`() = testPerioderEndpoint {
        coEvery { ressursService.opprett(any()) }.throws(Exception())

        client.testPost("/dagpenger/v1/periode", enDatadelingRequest(), server.createToken()).apply {
            assertEquals(HttpStatusCode.InternalServerError, this.status)
        }
    }*/
}