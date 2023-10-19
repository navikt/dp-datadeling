package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.*
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class RessursE2ETest : AbstractE2ETest() {
    private lateinit var ressursService: RessursService

    @BeforeAll
    fun setup() {
        ressursService = RessursService(RessursDao(database.dataSource))
    }

    @Test
    fun `opprett ressurs og poll til ressurs har status FERDIG`() = runBlocking {
        val request = DatadelingRequest(
            personIdent = "123",
            fraOgMedDato = 1.januar(),
            tilOgMedDato = 31.januar(),
        )

        val ressursUrl = client.postJson("/dagpenger/v1/periode", objectMapper.writeValueAsString(request))
            .apply { assertEquals(HttpStatusCode.Created, this.status) }
            .bodyAsText()

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.OPPRETTET, this.status)
            assertNull(this.data)
        }

        val response = DatadelingResponse(
            personIdent = request.personIdent,
            perioder = listOf(
                Periode(
                    fraOgMedDato = 10.januar(),
                    tilOgMedDato = 25.januar(),
                    ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    gjenståendeDager = 100,
                )
            )
        )
        ressursService.ferdigstill(1L, response)

        ressursUrl.fetchRessursResponse {
            assertEquals(RessursStatus.FERDIG, this.status)
            assertEquals(response, this.data)
        }
    }

    private suspend fun String.fetchRessursResponse(block: Ressurs.() -> Unit) {
        client.get(this)
            .apply { assertEquals(HttpStatusCode.OK, this.status) }
            .let { objectMapper.readValue(it.bodyAsText(), Ressurs::class.java) }
            .apply { block() }
    }

    private suspend fun HttpClient.postJson(path: String, body: String) = this.post(path) {
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(body)
    }
}

