package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ManuellE2ETest {
    val client =
        HttpClient {
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    data class Token(
        val access_token: String,
    )

    fun tokenProvider(): String {
        return runBlocking {
            client.get("https://dp-maskinporten-client.intern.dev.nav.no/token").bodyAsText().let {
                objectMapper.readValue(it, Token::class.java).access_token
            }
        }
    }

    //    @Disabled
    @Test
    fun bubba() {
        runBlocking {
            val request =
                DatadelingRequest(
                    personIdent = "02929898071",
                    fraOgMedDato = LocalDate.of(2023, 10, 1),
                )
            val token = tokenProvider()
            val ressursUrl =
                client.post("https://dp-datadeling.ekstern.dev.nav.no/dagpenger/v1/periode") {
                    headers {
                        append(HttpHeaders.Accept, ContentType.Application.Json)
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        bearerAuth(token)
                    }
                    setBody(objectMapper.writeValueAsString(request))
                }.bodyAsText().also {
                    println("Ressurs url: $it")
                }

            repeat((1..5).count()) { i ->
                println("Henter ressurs: $ressursUrl")
                client.get(ressursUrl) {
                    bearerAuth(token)
                }.let { response ->
                    println("Status: " + response.status)
                    response.bodyAsText().also { println("Resultat $i: $it") }
                }
                delay(1000)
            }
        }
    }
}
