package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.januar
import no.nav.dagpenger.datadeling.ressurs.Ressurs
import no.nav.dagpenger.datadeling.ressurs.RessursDao
import no.nav.dagpenger.datadeling.ressurs.RessursService
import no.nav.dagpenger.datadeling.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class RessursE2ETest : AbstractE2ETest() {
    private lateinit var ressursService: RessursService

    @BeforeAll
    fun setup() {
        ressursService = RessursService(RessursDao(dataSource))
    }

    @Test
    fun `opprett ressurs og poll til ressurs har status FERDIG`() = runBlocking {
        val response = DatadelingResponse(
            personIdent = "123",
            perioder = listOf(
                Periode(
                    fraOgMedDato = 10.januar(),
                    tilOgMedDato = 25.januar(),
                    ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    gjenståendeDager = 100,
                )
            )
        )

        mockIverksettResponse(response)
        mockProxyResponse(response)

        val request = DatadelingRequest(
            personIdent = response.personIdent,
            fraOgMedDato = 1.januar(),
            tilOgMedDato = 31.januar(),
        )

        val ressursUrl = client.post("/dagpenger/v1/periode") {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append(HttpHeaders.Authorization, "Bearer  $token")
            }
            setBody(objectMapper.writeValueAsString(request))
        }
            .apply { assertEquals(HttpStatusCode.Created, this.status) }
            .bodyAsText()

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.OPPRETTET, this.status)
            assertNull(this.data)
        }

        runBlocking {
            await.until {
                ressursService.hent(1L)?.status == RessursStatus.FERDIG
            }
        }

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.FERDIG, this.status)
            assertEquals(response, this.data)
        }
    }

    private suspend fun String.fetchRessursResponse(block: Ressurs.() -> Unit) {
        client.get(this) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
            .apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let { objectMapper.readValue(it.bodyAsText(), Ressurs::class.java) }
            .apply { block() }
    }

}

