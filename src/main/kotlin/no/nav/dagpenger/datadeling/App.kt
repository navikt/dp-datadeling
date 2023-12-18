package no.nav.dagpenger.datadeling

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.datadeling.api.datadelingApi

val defaultLogger = KotlinLogging.logger {}

fun main() {
    runMigration()
    createDatadelingServer().start(wait = true)
}

fun createDatadelingServer(port: Int = 8080): CIOApplicationEngine {
    return embeddedServer(port = port, module = { datadelingApi() }, factory = CIO)
}
