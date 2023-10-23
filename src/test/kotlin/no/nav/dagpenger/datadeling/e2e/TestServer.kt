package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.datadeling.testModule
import java.net.ServerSocket
import javax.sql.DataSource

class TestServer(private val dataSource: DataSource, ) {
    fun start(): TestServerRuntime = TestServerRuntime(dataSource)
}

class TestServerRuntime(
    dataSource: DataSource,
    private val httpPort: Int = ServerSocket(0).use { it.localPort },
) : AutoCloseable {
    private val server = createEmbeddedServer(
        dataSource = dataSource,
        httpPort = httpPort,
    )

    companion object {
        private fun createEmbeddedServer(
            dataSource: DataSource,
            httpPort: Int,
        ) =
            embeddedServer(CIO, applicationEngineEnvironment {
                connector { port = httpPort }
                module {
                    testModule(dataSource = dataSource, port = httpPort)
                }
            })
    }

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
                port = httpPort
            }
            expectSuccess = false
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper)
                )
            }
        }
    }
}
