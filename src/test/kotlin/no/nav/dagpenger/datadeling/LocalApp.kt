package no.nav.dagpenger.datadeling

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.testModule() {
    System.setProperty("DP_IVERKSETT_URL", "http://localhost:8094/api")
    System.setProperty("DP_IVERKSETT_SCOPE", "some-scope")
    System.setProperty("DP_PROXY_URL", "http://0.0.0.0:8092/api")
    System.setProperty("DP_PROXY_SCOPE", "some-scope")
    module()
}
