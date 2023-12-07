package no.nav.dagpenger.datadeling.api.perioder.ressurs

import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.objectMapper
import java.net.InetAddress.getLocalHost

class LeaderElector(private val httpClient: HttpClient, private val appConfig: AppConfig) {
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