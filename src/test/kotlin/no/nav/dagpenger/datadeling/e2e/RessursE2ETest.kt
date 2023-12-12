package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.TestApplication
import no.nav.dagpenger.datadeling.api.perioder.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testutil.januar
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertNull

class RessursE2ETest : AbstractE2ETest() {
    private lateinit var ressursService: RessursService

    @BeforeAll
    fun setup() {
        ressursService = RessursService(
            ressursDao = RessursDao(Config.datasource),
            leaderElector = mockk(relaxed = true),
            config = Config.appConfig.ressurs,
        )
    }

    @Test
    fun `opprett ressurs og poll til ressurs har status FERDIG`() = runBlocking {
        val response = DatadelingResponse(
            personIdent = "123", perioder = listOf(
                Periode(
                    fraOgMedDato = 10.januar(),
                    tilOgMedDato = 25.januar(),
                    ytelseType = DAGPENGER_ARBEIDSSOKER_ORDINAER,
                )
            )
        )

        mockProxyResponse(response, delayMs = 200)

        val request = DatadelingRequest(
            personIdent = response.personIdent,
            fraOgMedDato = 1.januar(),
            tilOgMedDato = 31.januar(),
        )

        val ressursUrl = client.post("/dagpenger/v1/periode") {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                bearerAuth(TestApplication.issueMaskinportenToken())
            }
            setBody(objectMapper.writeValueAsString(request))
        }.apply { assertEquals(HttpStatusCode.Created, this.status) }.bodyAsText()

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.OPPRETTET, this.status)
            assertNull(this.response)
        }

        val uuid = UUID.fromString(ressursUrl.split("/").last())
        runBlocking {
            await.until {
                ressursService.hent(uuid)?.status == RessursStatus.FERDIG
            }
        }

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.FERDIG, this.status)
            assertEquals(response, this.response)
        }
    }

    @Test
    fun `opprett ressurs og marker som FEILET ved error fra baksystem`() = runTest {
        val response = DatadelingResponse(
            personIdent = "123", perioder = listOf(
                Periode(
                    fraOgMedDato = 10.januar(),
                    tilOgMedDato = 25.januar(),
                    ytelseType = DAGPENGER_ARBEIDSSOKER_ORDINAER,
                )
            )
        )

        mockProxyError()

        val request = DatadelingRequest(
            personIdent = response.personIdent,
            fraOgMedDato = 1.januar(),
            tilOgMedDato = 31.januar(),
        )

        val ressursUrl = client.post("/dagpenger/v1/periode") {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                bearerAuth(TestApplication.issueMaskinportenToken())
            }
            setBody(objectMapper.writeValueAsString(request))
        }.bodyAsText()

        val uuid = ressursUrl.let {
            try {
                UUID.fromString(ressursUrl.split("/").last())
            } catch (e: Exception) {
                throw RuntimeException("Kunne ikke hente uuid fra ressursUrl: $ressursUrl")
            }
        }

        runBlocking {
            await.atMost(Duration.ofSeconds(5)).until {
                ressursService.hent(uuid)?.status == RessursStatus.FEILET

            }
        }

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.FEILET, this.status)
        }
    }

    private suspend fun String.fetchRessursResponse(block: Ressurs.() -> Unit) {
        client.get(this) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.Authorization, "Bearer ${TestApplication.issueMaskinportenToken()}")
            }
        }.apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let { objectMapper.readValue(it.bodyAsText(), Ressurs::class.java) }.apply { block() }
    }
}

