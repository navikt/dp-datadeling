package no.nav.dagpenger.datadeling.ressurs

import io.ktor.server.config.*

data class RessursConfig(
    val levetidMinutter: Long,
    val oppryddingsfrekvensMinutter: Long,
) {
    companion object {
        fun fra(config: ApplicationConfig) = RessursConfig(
            levetidMinutter = config.property("ressurs.levetidMinutter").getString().toLong(),
            oppryddingsfrekvensMinutter = config.property("ressurs.oppryddingsfrekvensMinutter").getString().toLong()
        )
    }
}