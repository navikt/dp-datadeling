package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.datadeling.createDatadelingServer
import no.nav.dagpenger.datadeling.objectMapper

class TestServer {
    fun start(): TestServerRuntime = TestServerRuntime()
}

const val SERVER_PORT = 8080

class TestServerRuntime : AutoCloseable {
    private val server = createDatadelingServer(port = SERVER_PORT)

    init {
        server.start(wait = false)
    }

    override fun close() {
        server.stop(0, 0)
    }

    fun restClient(): HttpClient {
        return HttpClient {
            defaultRequest {
                host = "localhost"
                port = SERVER_PORT
            }
            expectSuccess = false
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper)
                )
            }
        }
    }
}
