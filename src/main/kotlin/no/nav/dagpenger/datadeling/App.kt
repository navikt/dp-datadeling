package no.nav.dagpenger.datadeling

import mu.KotlinLogging

val defaultLogger = KotlinLogging.logger {}

fun main() {
    ApplicationBuilder(Config.asMap()).start()
}
