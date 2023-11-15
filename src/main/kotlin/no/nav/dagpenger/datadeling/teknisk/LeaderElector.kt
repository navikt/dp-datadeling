package no.nav.dagpenger.datadeling.teknisk

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.net.InetAddress.getLocalHost

class LeaderElector(private val httpClient: HttpClient) {
    fun isLeader() = runBlocking {
        val electorPath = System.getenv("ELECTOR_PATH")
        val leaderName = httpClient.request("http://$electorPath").bodyAsText()
            .let { objectMapper.readTree(it).get("name").asText() }
        val hostname: String = getLocalHost().hostName
        hostname == leaderName
    }
}