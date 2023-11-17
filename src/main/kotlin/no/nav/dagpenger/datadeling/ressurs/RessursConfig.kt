package no.nav.dagpenger.datadeling.ressurs

import io.ktor.server.config.*

data class RessursConfig(
    val minutterLevetidOpprettet: Long,
    val minutterLevetidFerdig: Long,
    val oppryddingsfrekvensMinutter: Long,
) {
    companion object {
        fun fra(config: ApplicationConfig) = RessursConfig(
            minutterLevetidOpprettet = config.property("ressurs.minutterLevetidOpprettet").getString().toLong(),
            minutterLevetidFerdig = config.property("ressurs.minutterLevetidFerdig").getString().toLong(),
            oppryddingsfrekvensMinutter = config.property("ressurs.oppryddingsfrekvensMinutter").getString().toLong()
        )
    }
}