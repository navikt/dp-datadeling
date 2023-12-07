package no.nav.dagpenger.datadeling

import io.ktor.server.netty.EngineMain
import mu.KotlinLogging

val defaultLogger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = EngineMain.main(args)
