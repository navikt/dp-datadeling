package no.nav.dagpenger.datadeling

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused") // application.yaml refererer til modulen
fun Application.testApplication() {
    System.setProperty("DP_IVERKSETT_URL", "http://localhost:8094/api")
    System.setProperty("DP_IVERKSETT_SCOPE", "some-scope")
    System.setProperty("DP_PROXY_URL", "http://localhost:8081/api")
    System.setProperty("DP_PROXY_SCOPE", "some-scope")
    module()
}
