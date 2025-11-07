package no.nav.dagpenger.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging

val defaultLogger = KotlinLogging.logger {}

fun main() {
    ApplicationBuilder(Config.asMap()).start()
}
