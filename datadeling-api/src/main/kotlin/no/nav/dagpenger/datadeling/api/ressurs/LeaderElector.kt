package no.nav.dagpenger.datadeling.api.ressurs

import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.AppConfig
import no.nav.dagpenger.datadeling.Config.electorPathUrl
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.ktor.client.defaultHttpClient
import java.net.InetAddress.getLocalHost

class LeaderElector(
    private val appConfig: AppConfig,
) {
    fun isLeader() =
        runBlocking {
            if (appConfig.isLocal) {
                return@runBlocking true
            }
            val leaderName =
                defaultHttpClient
                    .request(electorPathUrl())
                    .bodyAsText()
                    .let { objectMapper.readTree(it).get("name").asText() }
            val hostname: String = getLocalHost().hostName
            hostname == leaderName
        }
}
