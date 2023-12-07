package no.nav.dagpenger.datadeling.e2e

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testModule
import javax.sql.DataSource

class TestServer(private val dataSource: DataSource) {
    fun start(config: AppConfig, port: Int): TestServerRuntime = TestServerRuntime(dataSource, config, port)
}

const val SERVER_PORT = 9080

class TestServerRuntime(
    dataSource: DataSource,
    config: AppConfig,
    private val httpPort: Int,
) : AutoCloseable {
    private val server = createEmbeddedServer(
        dataSource = dataSource,
        httpPort = SERVER_PORT,
        config = config,
    )

    companion object {
        private fun createEmbeddedServer(
            dataSource: DataSource,
            httpPort: Int,
            config: AppConfig,
        ) =
            embeddedServer(CIO, applicationEngineEnvironment {
                connector { port = httpPort }
                module {
                    testModule(dataSource, config)
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
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper)
                )
            }
        }
    }
}
