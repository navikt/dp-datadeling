package no.nav.dagpenger.datadeling.api.perioder.ressurs

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.api.installRetryClient
import no.nav.dagpenger.datadeling.objectMapper
import java.net.InetAddress.getLocalHost

class LeaderElector(private val appConfig: AppConfig) {
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        installRetryClient(maksRetries = 5)
    }

    fun isLeader() = runBlocking {
        if (appConfig.isLocal) {
            return@runBlocking true
        }
        val electorPath = System.getenv("ELECTOR_PATH")
        val leaderName = httpClient.request("http://$electorPath").bodyAsText()
            .let { objectMapper.readTree(it).get("name").asText() }
        val hostname: String = getLocalHost().hostName
        hostname == leaderName
    }
}