package no.nav.dagpenger.datadeling

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.api.datadelingApi

val defaultLogger = KotlinLogging.logger {}

fun main() {
    val datadelingApi: Application.() -> Unit = {
        datadelingApi(
        )
    }
    embeddedServer(port = 8080, module = datadelingApi, factory = CIO).start(wait = true)
}
